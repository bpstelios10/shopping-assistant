# Streaming

The chat API has two shapes of the same turn:

| Endpoint | Response | Use |
|----------|----------|-----|
| `POST /chat` | one `ChatReplyDto` | integrations, evaluation suites, anything that wants the whole answer |
| `POST /chat/stream` | `text/event-stream` | shopper-facing UI |

Both go through the same `AgentOrchestrator` → `RouterAgent` → agent path. Streaming is a transport concern, not a
second pipeline.

## Event protocol

The stream is **typed events, not raw tokens**. A shopper-facing stream has to carry more than text — which agent
answered, which tools ran, what the turn cost — and encoding that into the text would be a protocol in disguise.

| SSE `event` | Payload | Notes |
|-------------|---------|-------|
| `routed` | `agent`, `conversationId`, `confidence` | first event, before any token |
| `delta` | `text` | incremental assistant text |
| `tool` | `id`, `type`, `name`, `arguments` | a tool the model invoked |
| `done` | `model`, `promptTokens`, `completionTokens` | terminal success |
| `error` | `message` | terminal failure |

Ordering for a successful turn: `routed` → zero or more `tool`/`delta` → exactly one `done`.
A failed turn ends with `error` instead, and nothing follows it.

Each payload also repeats its own name in an `event` field, so clients using `onmessage` (rather than
`addEventListener` per type) can still discriminate.

```shell
curl -N -X POST http://localhost:8080/shopping-assistant/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"message":"where is my order 123?","conversationId":"c1"}'
```

## Design notes

**Errors are in-band.** Once the response is committed there is no status code left to fail with, so
`ControllerExceptionHandler` cannot help. `ChatServiceImpl` converts any failure into a terminal `error` event and
completes the flux normally. Validation still returns a normal 4xx, because `@Valid` runs before the flux is
subscribed.

**Routing runs off the servlet thread.** The router is a full blocking inference. `AgentOrchestrator.chatStream`
wraps it in `Mono.fromCallable(...).subscribeOn(boundedElastic())` so the container thread is released immediately
instead of being held for the routing round-trip.

**The flux is cold.** Nothing reaches the model until subscription, and a shopper closing the tab cancels the
subscription, which propagates upstream and abandons the inference.

**There is a stream timeout** (`shopping-assistant.chat.stream-timeout`, default 60s). A stalled inference would
otherwise hold the connection — and its async context — open indefinitely.

## What streaming forced to change

Streaming is not additive; it breaks anything that assumed one request equals one thread.

- **`CurrentUser` (ThreadLocal) had to go.** Tools execute on Reactor threads, long after the request thread has
  cleared the value. The user id is now carried explicitly in `AgentRequest` and handed to tools through Spring AI's
  `ToolContext` (see `tools.md`), which travels with the request rather than the thread.
- **`ToolCallAuditingAdvisor` had to implement `StreamAdvisor`.** An advisor that only implements `CallAdvisor` is
  silently skipped on the streaming path — tool auditing would have quietly stopped working.
- **The tool-call audit list must be thread-safe**, because it is appended to from Reactor threads.
- **Usage is normalised.** Providers send usage only on the final chunk and some send none at all. A response with
  no metadata reports model `""` and `0` tokens, which reads as a free call rather than as missing data, so
  `ChatClientAgent` maps "no usage reported" to nulls.

## Known gap

`SummaryBufferChatMemory.add()` still calls the model synchronously when it compacts. On the blocking path that is a
p99 latency cliff; on the streaming path it also **blocks a Reactor thread**, because the memory advisor aggregates
and persists on the thread that emits completion. Moving summarization off the request path is the top item in
`open-tasks.md` and matters more now than it did before.

