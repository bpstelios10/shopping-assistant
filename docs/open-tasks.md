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

- [ ] **Replace `CurrentUser` ThreadLocal with Spring AI `ToolContext`** — thread-safe, explicit, removes static
  setup/teardown from tests.
- [ ] **Timeout + circuit breaker on the model call.** Downstream REST clients have both; the LLM (slowest, most
  failure-prone dependency) has neither. With `tomcat.threads.max=500`, a hung inference endpoint exhausts the pool.
- [ ] **Cap tool-call iterations per turn.** Nothing bounds tool ping-pong today — the "do not call this tool again"
  text in tool descriptions is a prompt-level workaround for a missing runtime guard.
- [ ] **Cheap router short-circuit.** Greetings and obvious keyword matches shouldn't cost an inference; cache routing
  decisions on a normalized-message hash.
- [ ] **Idempotent RAG ingestion.** `DELETE FROM vector_store` + reload leaves a window with no knowledge base and
  re-embeds unchanged files. Hash per document, skip unchanged, replace per document. Add doc version to chunk metadata.
- [x] **Move `RagIngestionRunner` out of the scanned package** — a second `@SpringBootApplication` under the main
  package is a component-scan/context-resolution hazard.

## Code quality

- [x] **Extract `AbstractAgent`.** The three agents differ only in name, prompt provider and tools; everything else is
  copy-paste, including the audit-list seeding that callers must remember to do. Replace
  `RuntimeException("Agent didnt reply")` with a domain exception mapped in `ControllerExceptionHandler`.
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
- [ ] Replace `org.apache.logging.log4j.util.Strings` with `String.isBlank()` / `String.join()`.

## Notes

The `LATEST_TURN_GUARD` in `PromptDecorator`, the duplicate guard in the RAG prompt template, and the anti-repetition
text in tool descriptions are all workarounds for weak instruction-following in the local 8B model — not permanent
design. Revisit when the target model changes.

