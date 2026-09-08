# Shopping Assistant

An AI-powered shopping assistant built with **Spring AI** to explore modern **LLM Agent** concepts in a real-world
microservice architecture.

The goal of this project is not only to integrate an LLM into an application, but also to understand how
production-ready AI assistants are designed and implemented.

## Features

The assistant is built as a **multi-agent system**: a router classifies the request and delegates it to a
role-specialized agent that owns its own prompt, tools and knowledge sources.

| Capability | Status | Notes |
|---|---|---|
| 🧠 **LLM Agent** | ✅ Active | Router + Shopping / Order / Support agents |
| 🛠️ **Tool Calling** | ✅ Active | Products & Orders APIs, preference capture |
| 💬 **Memory** | ✅ Active | Redis conversation memory + durable Postgres user profile |
| 📚 **RAG** | ✅ Active | Policy/FAQ answers over ingested documents (pgvector) |
| 🔍 **Observability** | ✅ Active | Actuator + Prometheus, prompt/tool-call auditing |
| 🤖 **Model-Agnostic Design** | ✅ Active | OpenAI-compatible API (Ollama, vLLM, ...) |
| 🧪 **AI Evaluation Tests** | ✅ Active | Prompt/model regression suites per agent |
| 📄 **Structured Output** | 🚧 Partial | Typed API DTOs; stricter model-level contracts next |
| ⚡ **Streaming** | ✅ Active | `POST /chat/stream` — typed SSE events (routing, tokens, tool calls, usage) |

## Architecture

```text
                          React Store
                               │
                               ▼
                    ┌────────────────────┐
                    │ Shopping Assistant │   Spring AI
                    └─────────┬──────────┘
                              ▼
                     Agent Orchestrator
                              ▼
                        Router Agent
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
   Shopping Agent        Order Agent        Support Agent
          │                   │                   │
          ▼                   ▼                   ▼
  Products GO Service  Orders GO Service     pgvector (RAG)

  Shared infra: Redis (chat memory) · Postgres (user memory + vectors)
                Observability · LLM + Embedding model
                               │
                    OpenAI-compatible API
                               │
                 ┌─────────────┴─────────────┐
              Ollama (local)            vLLM (production)
```

Details: [architecture](docs/architecture.md).

### Known limitations

- Only the **first** routing decision of a plan is executed (multi-step execution pending).
- User identity is placeholder-based (`anon:sess-abc`) until auth is wired in.
- Token usage is not yet aggregated across router + agent calls.

## Docs

- [Architecture design](docs/architecture.md)
- [Tools design](docs/tools.md)
- [Streaming design](docs/streaming.md)
- [Memory design](docs/memory.md)
- [RAG ingestion flow](docs/RAG.md)
- [Prompts flow](docs/prompts.md)
- [Metrics](docs/metrics.md)
- [Caching design](docs/caching.md)
- [AI evaluation testing](docs/ai-evaluation-testing.md)
- [Roadmap (current state + remaining work)](docs/roadmap.md)
- [Open tasks (hardening backlog)](docs/open-tasks.md)
- [Local setup and run guide](docs/local-run.md)

## Quick Start

Prerequisites: **Java 25** and **Docker Compose**. Ollama runs in Docker on Linux/Windows and natively on Mac
(Apple Silicon) for GPU acceleration. Models used: `qwen3:8b` (chat) and `nomic-embed-text` (embeddings).

```shell
# Linux / Windows — Ollama runs in Docker
docker compose up -d

# Mac (Apple Silicon) — run Ollama natively for GPU acceleration
ollama serve &
docker compose -f docker-compose.yml -f docker-compose.mac.yml up -d postgres redis
```

Then start the service:

```shell
cd shopping-assistant
./gradlew bootRun
```

Verify:

```shell
curl -X POST http://localhost:8080/shopping-assistant/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"I need running shoes under 120","conversationId":"demo-1"}'

curl http://localhost:8081/shopping-assistant/private/health
```

| Port | Purpose |
|---|---|
| `8080` | Application API (`/shopping-assistant`) |
| `8081` | Management endpoints (`/shopping-assistant/private`) |
| `5432` | Postgres + pgvector |
| `6379` | Redis Stack |
| `11434` | Ollama |

Full instructions: [local run guide](docs/local-run.md).

## Tests

```shell
cd shopping-assistant
./gradlew test              # unit + component tests (integration excluded)
./gradlew integrationTest   # integration + AI evaluation tests (needs dependencies running)
```

Postman collection: [`postman/Shopping-Assistant.postman_collection.json`](postman/Shopping-Assistant.postman_collection.json)
