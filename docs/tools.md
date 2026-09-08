# Tools

Tools are the agents' way of reaching business logic: the model decides *whether* to call one and *with what
arguments*, the application decides what it is *allowed* to get back.

## Available tools

| Tool                                                                  | Agents           | Backing service      |
|-----------------------------------------------------------------------|------------------|----------------------|
| `ProductTool` — `listAllProducts`, `getProductById`, `searchProducts` | shopping         | Products service     |
| `CategoryTool` — `listCategories`                                     | shopping         | Products service     |
| `OrderTool`                                                           | orders           | Orders service       |
| `UserPreferenceTool` — `saveUserPreference`                           | shopping, orders | Postgres user memory |

Tools are wired by marker interfaces (`ShoppingAgentTool`, `OrderAgentTool`), so an agent receives exactly the tools
it is allowed to use and adding a tool to an agent is a matter of implementing the right interface.

## Failure handling

`ToolExecutionExceptionProcessor` (in `AiConfig`) converts any tool exception into a short, bounded instruction for
the model instead of the raw exception text. Spring AI's default behavior feeds the stack message back into the
conversation, which makes the model flail and re-open earlier questions. The replacement message tells it to admit
the failure and stay on the current turn.

## Loop bounding

`BoundedToolCallingManager` decorates the `ToolCallingManager` and caps the number of tool round-trips per user turn
(`shopping-assistant.tools.max-iterations`, default 5).

Spring AI's tool loop ends only when the model stops asking for tools, so an oscillating model (typically
`searchProducts` → empty → `listCategories` → `searchProducts` → ...) has no upper bound on cost or latency, and
each round is more expensive than the last as the history grows.

The iteration count is read from the prompt — one `ToolResponseMessage` per completed round — so the decorator holds
no state and is thread-safe. On breach it degrades in two stages rather than throwing:

1. at the limit, tools are not executed and the model is asked to answer with what it has;
2. one round later, the loop is terminated unconditionally (`returnDirect`) and a fallback reply is returned.

An exception would mean a 500 after the shopper already paid for several inferences, which is strictly worse than a
partial answer.

## Request context

Tools are singletons, but they need per-request state (the caller's id). That is passed through Spring AI's
`ToolContext` — see `ToolContextValues` — which the agent populates via `.toolContext(...)`:

```java
public String saveUserPreference(String key, String value, ToolContext toolContext) {
    Optional<String> userId = ToolContextValues.userId(toolContext);
```

Two reasons this is not a `ThreadLocal`:

- The `ToolContext` parameter is **not part of the schema advertised to the model**, so the caller identity cannot be
  spoofed through a tool argument — unlike passing `userId` as a `@ToolParam`.
- It travels with the request, not the thread, so it survives the thread hops that streaming and tool execution
  introduce. A `ThreadLocal` set on the servlet thread is already cleared by the time a Reactor thread executes the
  tool (see `streaming.md`).

## Auditing

`ToolCallAuditingAdvisor` records which tools were actually invoked during a turn, so the reply can report them and
the evaluation suites can assert on them.

It implements both `CallAdvisor` and `StreamAdvisor`, ordered at `HIGHEST_PRECEDENCE + 400` — immediately after
Spring AI's `ToolCallingAdvisor` (`+ 300`) — so it observes the response once tool calls have been resolved. An
advisor that implements only the call side is silently skipped when streaming, which would make tool auditing
disappear without any error.

The executed calls are appended to a mutable list that the caller places in the advisor context under
`TOOL_CALLS_CONTEXT_KEY`; `ChatClientAgent` seeds it on every request with a **synchronized** list, because under
streaming the advisor appends from Reactor threads. If the key is missing or unusable the update is skipped and
logged rather than failing the call — auditing must never break a conversation.

> The seeding requirement is a leaky contract: it works only because every agent goes through `ChatClientAgent`.
> See `open-tasks.md`.

## Conventions

- **Enforce limits in code, not in the prompt.** A prompt limit is a request the model may ignore; anything
  protecting context, cost or latency belongs on the boundary.
- **Push constraints down to the service.** `RestProductClient` sends `limit` to the Products service so the payload
  is bounded before it is transferred, not after.
- **Tool descriptions are for the model, not for enforcement.** They should describe behavior (including limits) so
  the model reasons correctly about the result.
- Several descriptions currently contain anti-repetition instructions (e.g. *"Do not call this tool again for the
  same params in the same turn"*). These are workarounds for the local model's weak instruction-following, not
  permanent design — the loop cap is the real guard.
- **Don't return raw exceptions to the model.** The model is not a debugger; it should be told that a tool failed
  and why, but not given the stack trace or the full exception message.
