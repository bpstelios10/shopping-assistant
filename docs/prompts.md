# Prompts

Short notes on where prompts live and how they're built.

## Location

- System prompt templates: `src/main/resources/prompts/`
  - `shopping-system.st` — shopping agent (product discovery, tool-aware).
  - `order-system.st` — orders agent (order discovery, tool-aware).
  - `support-system.st` — support agent (policy/FAQ, RAG-grounded).

## Composition

Each agent has a `PromptProvider` (e.g. `ShoppingPromptProvider`, `SupportPromptProvider`)
that builds its `Prompt` via `PromptDecorator`:

```
system template  → user preferences (if any)  → user message  → latest-turn guard (system)
```

The memory advisor then inserts the conversation history (summary + recent window) before the prompt messages,
and for the support agent the RAG advisor augments the user message with retrieved context.

- `AbstractPromptProvider.getUserPreferences()` adds a `Known information about the user: ...`
  `SystemMessage` when a user profile exists (empty → skipped).
- `PromptDecorator.build()` appends `LATEST_TURN_GUARD` as a trailing `SystemMessage` — a workaround for the local
  model's weak instruction-following.
- Template vars (`{today}`, `{language}`, `{storeName}`) are rendered per request; `{categories}` only for shopping.
  `storeName` and `language` are currently hardcoded in the providers.

## Router prompt

`RouterAgent` uses an inline system prompt (not a `.st` file) to classify the request into
`SHOPPING` / `ORDERS` / `SUPPORT` steps, each with a task and a confidence score.

## Conventions

- Keep prompts small; prefer explicit, low-ambiguity instructions (weak local models).
- Prompt content is agent-specific — no shared "god prompt".

## TODO

- Externalize prompts to a versioned prompt-management system (editable, A/B-tested,
  rolled out without redeploy).
