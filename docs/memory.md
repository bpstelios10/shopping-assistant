# Memory

The assistant has two runtime memory tiers. These are separate from RAG (see `RAG.md`),
which is knowledge-base retrieval over ingested documents, not memory.

| Tier | Scope | Store | Retrieval | Lifetime |
|------|-------|-------|-----------|----------|
| Conversation (window + summary) | `conversationId` | Redis | chronological | 14-day TTL |
| User profile | `userId` | Postgres `jsonb` | key lookup | durable |

## Conversation memory — summary buffer

Implemented by `SummaryBufferChatMemory`
(`shopping-assistant/src/main/java/org/learnings/ai/shoppingassistant/services/memory/SummaryBufferChatMemory.java`).

### Behavior
- Keep the latest **10 raw messages** (~5 turns; each turn is a question + an answer).
- Keep older messages as a single **running summary** (`SystemMessage` prefixed with
  `Summary of earlier conversation:`).
- Trigger summarization only when raw messages reach **20**, then fold the oldest down and
  keep the latest 10 raw. One LLM summary call per ~10 messages, not per turn.

### Stored shape per conversation
- `summary (optional) + last 10 raw messages`, all under the same `conversationId` in Redis.

### Tuning knobs
- `windowSize` (now `10`): how many raw messages are always sent.
- `bufferSize` (now `20`): when to compact.
- Rule: `bufferSize` must be greater than `windowSize`.

### Why
- Avoids summarizing every turn.
- Preserves recent details for follow-up questions.
- Reduces token usage for long chats.

## Conversation identity

- The memory key is a composite: `{userId}:{conversationId}`.
- `userId` is resolved per request in `AgentOrchestrator` (currently `anon:sess-abc`; to be
  replaced by real auth later). This isolates each user's chats without changing the
  `ChatMemoryRepository` contract.

## User profile memory

Durable, cross-conversation preferences (currency, size, brand, language, shipping, etc.).

- Service: `UserMemoryService` / `UserMemoryServiceImpl`.
- Storage: Postgres `user_memory` table, `profile jsonb`, keyed by `user_id`
  (migration `db/migration/V1__create_user_memory.sql`). No embeddings — it is an exact
  key lookup, not a similarity search.
- **Read:** `AbstractPromptProvider` injects a `Known information about the user: ...` `SystemMessage`
  (via `PromptDecorator`) when a profile exists.
- **Write:** captured via the `saveUserPreference` tool (`UserPreferenceTool`), which the
  model calls only when the shopper states a lasting preference. The per-request `userId`
  reaches the singleton tool via the `CurrentUser` ThreadLocal (set/cleared in
  `AgentOrchestrator`).

### Why a tool (not per-message extraction)
- One inference — the tool call is part of the existing chat call, no extra LLM round-trip.
- Fires only when a preference is actually stated.
- Summary-based extraction was rejected: short conversations never summarize, so their
  preferences would be lost.

## Known limitations
- Preference capture depends on the model deciding to call the tool; weak models may miss
  some. A real/Testcontainers-Ollama integration test would verify this.
- Summary quality depends on the chat model (`qwen3:8b`).
- No semantic recall over past conversations (not needed for short shopping sessions).
