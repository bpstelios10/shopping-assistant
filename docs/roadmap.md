# Roadmap

## Spring AI Vision Coverage

This project is intentionally built to demonstrate practical Spring AI knowledge across the core LLM-app capabilities.

- **LLM Agent:** active, with role-specialized agents for shopping, orders, and support.
- **Tool Calling:** active, with product/order tools and preference-capture tooling.
- **Memory:** active, with conversation memory and durable user preference memory.
- **RAG:** active for support/policy-style queries over ingested knowledge documents.
- **Structured Output:** partially active (typed API DTOs); stricter model-level contracts are next.
- **Streaming:** planned next (current chat API is request/response).
- **Observability:** active via Actuator + Prometheus endpoint exposure.
- **Model-Agnostic Design:** active through OpenAI-compatible provider integration.

## Implementation Details (Low-Level)

Current implementation:

- **Routing:** `RouterAgent` + `AgentOrchestrator` with fallback threshold `0.6`.
- **Conversation memory:** Redis summary-buffer (`window=10`, `buffer=20`, `TTL=14d`).
- **User memory:** durable Postgres `jsonb` profile.
- **RAG tuning:** `topK=4`, `similarityThreshold=0.5`.
- **Tool resilience:** graceful failure handling via `ToolExecutionExceptionProcessor`.

Current constraints:

- Only the first planned routing decision is executed (full multi-step execution pending).
- User identity is still placeholder-based (`anon:sess-abc`).
- Streaming is not enabled yet.
- Token accounting does not yet aggregate router + final-agent usage into one response summary.

## Next Milestones

1. Add streaming chat responses end-to-end.
2. Add stricter structured-output contracts for core workflows.
3. Replace placeholder identity with auth-derived user context.
4. Evolve routing from single decision to multi-step/multi-agent execution.
