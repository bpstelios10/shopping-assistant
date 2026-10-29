# Open Tasks

Engineering backlog for hardening the assistant. Grouped by impact.
Separate from `roadmap.md`, which tracks feature-level milestones (streaming, guardrails, auth, multi-step execution).

## Correctness & cost

- [x] **Atomic profile merge.** `UserMemoryServiceImpl.updateProfile` does read-modify-write across two round trips —
  concurrent turns can drop a preference. Move the merge into Postgres:
  `SET profile = user_memory.profile || EXCLUDED.profile`.
- [ ] **Bound tool payloads.** `listAllProducts` / `searchProducts` serialize the full catalog into the context. Add a
  hard result cap, return a projection (`id, name, price, category`) instead of full `Product`, and signal truncation to
  the model.
  NOTE: this could be done at client level. put a limit and get a header back for the total count. but all responses
  then need to change... Another way is to say in the prompt or something 'oh if the result is 10, then say that there
  might be more'.
- [ ] **Move summarization off the request path.** `SummaryBufferChatMemory.add()` blocks on `chatModel.call()`, adding
  a full inference to every ~10th turn. Make it async.
  NOTE: could make the add async, but i need to handle race-conditions. so maybe a lock is needed. A per-conversation
  lock or a "summarization in progress" flag.
  NOTE 2: i can switch to `qwen2.5:3b-instruct`, but i need to add summarization eval cases first.
  NOTE 3: **more urgent since streaming landed** — the memory advisor aggregates and persists on the thread that emits
  completion, so this now blocks a Reactor thread, not just the servlet thread.
- [ ] **Deterministic prompt text.** `getProfileSummary` joins a `HashMap` — key order isn't stable, so the same profile
  renders differently across runs. Sort keys (breaks prompt caching and eval reproducibility).

## Evaluation harness

- [x] **Remove the dead assertion.** `SupportAgentEvaluationTest` asserts `groundedness.isPass()` *and* `score < 0.1` —
  can't both be meaningful.
- [x] **Revisit thresholds.** Shopping relevancy floor is `0.3` vs `0.9` elsewhere. Either justify it or record the gap
  as a finding.
- [ ] **k-sample runs + committed baseline.** Each case is currently a single draw from a distribution, and results only
  reach `log.warn`. Run k=5, assert on pass rate, write aggregates to a baseline file so prompt/model changes produce a
  diff.
- [ ] **Compare tool arguments as parsed JSON** in `ToolCallingEvaluator` (string equality is key-order sensitive);
  clamp score to `[0,1]`.

## Reliability

- [x] **Replace `CurrentUser` ThreadLocal with Spring AI `ToolContext`** — thread-safe, explicit, removes static
  setup/teardown from tests. Done as part of streaming: a ThreadLocal cannot survive the Reactor thread hops.
- [ ] **Timeout + circuit breaker on the model call.** Downstream REST clients have timeouts (no circuit breaker yet);
  the LLM (slowest, most failure-prone dependency) has neither. With `tomcat.threads.max=500`, a hung inference
  endpoint exhausts the pool.
- [x] **Cap tool-call iterations per turn.** Nothing bounds tool ping-pong today — the "do not call this tool again"
  text in tool descriptions is a prompt-level workaround for a missing runtime guard.
- [ ] **Cheap router short-circuit.** Greetings and obvious keyword matches shouldn't cost an inference; cache routing
  decisions on a normalized-message hash.
- [x] **Move `RagIngestionRunner` out of the scanned package** — a second `@SpringBootApplication` under the main
  package is a component-scan/context-resolution hazard.

## Code quality

- [x] **Extract `AbstractAgent`.** The three agents differ only in name, prompt provider and tools; everything else is
  copy-paste, including the audit-list seeding that callers must remember to do. Done via composition
  (`ChatClientAgent` + `AgentsConfig`).
- [ ] **Domain exception for "no reply".** `ChatClientAgent.chat` still throws a raw `RuntimeException`; replace with a
  domain exception mapped in `ControllerExceptionHandler` (e.g. 503).
- [ ] **Don't silently broaden searches.** `ProductTool.searchProducts` drops the query when a category search is empty,
  and the model presents the results as exact matches. Return a flag instead.
- [ ] **AI metrics.** Routing fallback rate, confidence histogram, agent distribution, tool error rate, tool-loop depth,
  empty-retrieval rate. Micrometer is already wired — without these there's no way to tell whether `0.6` is the right
  threshold.
- [ ] **Wire the stored `language` preference** into the prompt template; it's captured, persisted and then ignored (
  `ShoppingPromptProvider` hardcodes English). Move `storeName` to config.
- [ ] **Don't swallow Jackson failures** in `JdbcUserMemoryRepository` — a serialization failure currently persists
  `{}`, silently wiping the profile.
- [ ] **Null-safe `ChatReplyMapper`** — `metadata.getUsage()` isn't guaranteed populated across providers.
- [ ] **Externalize magic numbers** (window `10`, buffer `20`, threshold `0.6`, `topK=4`, temperatures) to
  `@ConfigurationProperties` — also enables parameter sweeps in the eval harness.
- [ ] **Move `extraBody(think=false)` to config** — Ollama-specific option hardcoded in a model-agnostic design.
- [x] Replace `org.apache.logging.log4j.util.Strings` with `String.isBlank()` / `String.join()`.

## Security & trust (from the Oct 2026 review — see `code-review.md`)

- [ ] **Order ownership.** `getOrderById` returns any order by UUID; `createOrder` attaches no user. Pass `userId` via
  `ToolContext` and enforce ownership.
- [ ] **Code-enforced confirmation + idempotency for `createOrder`.** Propose → confirm token, idempotency key, validate
  `quantity > 0` and an upper bound.
- [ ] **Preference/summary injection.** Allow-list preference keys, cap value length, and stop injecting user-derived
  content (profile, running summary) as `SystemMessage`.
- [ ] **Lock down management endpoints.** Drop `heapdump`/`env` exposure outside local.
- [ ] **Input limits + rate limiting** on `/chat` and `/chat/stream` (`@Size` on `message`/`conversationId`).
- [ ] **Don't leak internals to clients.** `reasoningContent` and tool arguments in `ChatReplyDto` should be debug-only.

## Correctness (from the Oct 2026 review)

- [ ] **Status handling in REST clients.** `exchange()` in `RestOrderClient` (both methods) and
  `RestProductClient.getProductById` parses 4xx/5xx bodies as domain objects.
- [ ] **Fix tool descriptions.** `searchProducts` references a non-existent `listCategories` tool; `getProductById`
  param says "UUID of the order"; `createOrder` param mentions "customer and products".
- [ ] **Advertised but missing order capabilities.** Order prompt + router offer cancel/history with no tools.
- [ ] **Money as `float`.** Use `BigDecimal`/minor units + currency in `Product`.
- [ ] **Unknown `OrderStatus` fails deserialization.** Add an `UNKNOWN` default; add SHIPPED/DELIVERED.
- [ ] **Router robustness.** Fallback on parse failure; guard `agent == null`; give the router recent history so
  follow-ups route correctly; keep the raw user message (not the rewritten task) in memory; don't silently drop steps
  2..n.
- [ ] **MDC/request-id lost on Reactor threads.** Enable Micrometer context-propagation so `X-Request-ID` reaches
  downstream calls and logs on the streaming path.
- [ ] **Concurrent `SummaryBufferChatMemory.add`** is read-modify-write on Redis — lost messages under concurrent turns.
- [ ] **Categories fetch fails the whole shopping turn** when the Products service is down — degrade to no categories.
- [ ] **Atomic RAG re-ingest** (versioned table / swap) instead of `DELETE` + insert.

## Housekeeping

- [ ] Remove dead code: `AgentTool`, `services/dtos/Message`, unused `AgentStreamEvent.Failed`, empty
  `prompts/recommendations.st`; stale `// TODO missing tests` in `ToolContextValues`; wrong `OrderClient` javadoc.
- [ ] `docker-compose.yml` `ollama-init`: missing `;` after the `nomic-embed-text` pull (folded `>` joins lines);
  pin `redis-stack` image.
- [ ] Apply `think=false` consistently (or via config) across agents; consider virtual threads instead of
  `tomcat.threads.max=500`.
- [ ] Move schema creation for pgvector/Redis index to migrations (`initialize-schema: true` today).

## Notes

The `LATEST_TURN_GUARD` in `PromptDecorator`, the duplicate guard in the RAG prompt template, and the anti-repetition
text in tool descriptions are all workarounds for weak instruction-following in the local 8B model — not permanent
design. Revisit when the target model changes.
