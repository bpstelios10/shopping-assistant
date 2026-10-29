# Roadmap

## Spring AI Vision Coverage

This project is intentionally built to demonstrate practical Spring AI knowledge across the core LLM-app capabilities.

- **LLM Agent:** active, with role-specialized agents for shopping, orders, and support.
- **Tool Calling:** active, with product/order tools and preference-capture tooling.
- **Memory:** active, with conversation memory and durable user preference memory.
- **RAG:** active for support/policy-style queries over ingested knowledge documents.
- **Structured Output:** partially active (typed API DTOs); stricter model-level contracts are next.
- **Streaming:** active — `POST /chat/stream` with typed SSE events (see `streaming.md`).
- **Observability:** active via Actuator + Prometheus endpoint exposure.
- **Model-Agnostic Design:** active through OpenAI-compatible provider integration.

## Implementation Details (Low-Level)

Current implementation:

- **Routing:** `RouterAgent` + `AgentOrchestrator` with fallback threshold `0.6`.
- **Conversation memory:** Redis summary-buffer (`window=10`, `buffer=20`, `TTL=14d`).
- **User memory:** durable Postgres `jsonb` profile.
- **RAG tuning:** `topK=4`, `similarityThreshold=0.5`.
- **Tool resilience:** graceful failure handling via `ToolExecutionExceptionProcessor`; tool loop capped by
  `BoundedToolCallingManager` (`max-iterations=5`).

Current constraints:

- Only the first planned routing decision is executed (full multi-step execution pending).
- User identity is still placeholder-based (`anon:sess-abc`).
- Token accounting does not yet aggregate router + final-agent usage into one response summary.

## Next Milestones

1. Replace placeholder identity with auth-derived user context (and per-user authorization in tools).
2. Add guardrails (input/output, prompt-injection defence for tool/RAG/memory content).
3. Add stricter structured-output contracts for core workflows.
4. Evolve routing from single decision to multi-step/multi-agent execution.
