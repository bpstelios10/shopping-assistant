# Prompts

Short notes on where prompts live and how they're built.

## Location

- System prompt templates: `src/main/resources/prompts/`
  - `shopping-system.st` — shopping agent (product discovery, tool-aware).
  - `support-system.st` — support agent (policy/FAQ, RAG-grounded).

## Composition

Each agent has a `PromptProvider` (e.g. `ShoppingPromptProvider`, `SupportPromptProvider`)
that builds its `Prompt` via `PromptDecorator`:

```
system template  → user preferences (if any)  → user message
```

- `AbstractPromptProvider.getUserPreferences()` adds a `Known information about the user: ...`
  `SystemMessage` when a user profile exists (empty → skipped).
- Template vars (`{today}`, `{language}`, `{storeName}`, `{categories}`) are rendered per request.

## Router prompt

`RouterAgent` uses an inline system prompt (not a `.st` file) to classify the request into
`SHOPPING` / `SUPPORT` with a confidence score.

## Conventions

- Keep prompts small; prefer explicit, low-ambiguity instructions (weak local models).
- Prompt content is agent-specific — no shared "god prompt".

## TODO

- Externalize prompts to a versioned prompt-management system (editable, A/B-tested,
  rolled out without redeploy).
