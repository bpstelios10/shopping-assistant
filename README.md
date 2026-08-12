# Shopping Assistant

An AI-powered shopping assistant built with **Spring AI** to explore modern **LLM Agent** concepts in a real-world
microservice architecture.

The goal of this project is not only to integrate an LLM into an application, but also to understand how
production-ready AI assistants are designed and implemented.

## Features

The assistant is built as an autonomous **LLM Agent** that interacts with services, retrieves knowledge, and
maintains conversational context.

We have (or are completing) all core building blocks of a modern AI application:

- 🧠 **LLM Agent** – autonomous reasoning and decision making
- 🛠️ **Tool Calling** – invoke APIs and business logic (Products, Orders, etc.)
- 💬 **Memory** – maintain conversation history and user preferences
- 📚 **RAG (Retrieval-Augmented Generation)** – answer questions using external documents and company knowledge
- 📄 **Structured Output** – generate typed JSON responses
- ⚡ **Streaming** – real-time responses
- 🔍 **Observability** – prompts, tool calls, latency and metrics
- 🤖 **Model Agnostic Design** – support multiple inference providers (Ollama, vLLM, OpenAI-compatible APIs)

The shopping assistant is already using these features in practice.
Implementation details and remaining work are tracked in the roadmap: `docs/roadmap.md`.

## Architecture

```text
                React Store
                     │
        ┌────────────┴────────────┐
        │                         │
   Products GO Service         Orders GO Service
              \             /
               \           /
                Shopping Assistant
                      │
                Spring AI Agent
                      │
            OpenAI-compatible API
                      │
        ┌─────────────┴─────────────┐
        │                           │
      Ollama (Local)         vLLM (Production)
```

## Docs

- Architecture design: `docs/architecture.md`
- Memory design: `docs/memory.md`
- RAG ingestion flow: `docs/RAG.md`
- Prompts flow: `docs/prompts.md`
- Caching design: `docs/caching.md`
- Roadmap (current state + remaining work): `docs/roadmap.md`
- Local setup and run guide: `docs/local-run.md`

## Quick Start

See full local instructions in `docs/local-run.md`.

```shell
docker compose up postgres redis -d
docker compose up --build
```

Postman collection: `postman/Shopping-Assistant.postman_collection.json`
