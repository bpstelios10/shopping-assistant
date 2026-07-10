# Shopping Assistant

An AI-powered shopping assistant built with **Spring AI** to explore modern **LLM Agent** concepts in a real-world
microservice architecture.

The goal of this project is not only to integrate an LLM into an application, but also to understand how
production-ready AI assistants are designed and implemented.

## Vision

The assistant will evolve from a simple chatbot into an autonomous **LLM Agent** capable of interacting with other
services, retrieving knowledge, and maintaining conversational context.

The project will gradually introduce the core building blocks of modern AI applications:

- 🧠 **LLM Agent** – autonomous reasoning and decision making
- 🛠️ **Tool Calling** – invoke APIs and business logic (Products, Orders, etc.)
- 💬 **Memory** – maintain conversation history and user preferences
- 📚 **RAG (Retrieval-Augmented Generation)** – answer questions using external documents and company knowledge
- 📄 **Structured Output** – generate typed JSON responses
- ⚡ **Streaming** – real-time responses
- 🔍 **Observability** – prompts, tool calls, latency and metrics
- 🤖 **Model Agnostic Design** – support multiple inference providers (Ollama, vLLM, OpenAI-compatible APIs)

## Architecture

```text
                React Store
                     │
        ┌────────────┴────────────┐
        │                         │
   Products Service         Orders Service
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

## 🎯 Learning Objectives

- Learn Spring AI
- Understand how LLM Agents work
- Build production-ready AI integrations
- Keep the inference layer replaceable
- Explore the complete AI application lifecycle, from local development to Kubernetes deployment

## BUILD + RUN

### Prerequisites

The app requires **PostgreSQL with pgvector** running before it starts. Start it with:

```shell
# Linux/Windows
docker compose up postgres -d

# Mac
docker compose -f docker-compose.yml -f docker-compose.mac.yml up postgres -d
```

The container uses `restart: unless-stopped` — you only need to do this once per machine.

### Linux / Windows (Ollama runs in Docker)

```shell
docker compose up --build
```

Ollama and the `qwen3:8b` model are managed entirely in Docker. The model is stored in a
named volume so it is only downloaded once.

### Mac — Apple Silicon (native Ollama)

Running Ollama inside Docker on macOS means CPU-only inference — no Metal/GPU access.
For full M-series performance, run Ollama natively and use the Mac override to skip the
Ollama containers:

```shell
# 1. Install Ollama natively (once)
#    Match the version in OLLAMA_VERSION in .env
#    Releases: https://github.com/ollama/ollama/releases
brew install ollama
brew services stop ollama    # stop and disable auto-start on login

# 2. Pull the model (once)
ollama serve &
ollama pull qwen3:8b
pkill ollama

# 3. Before running the app, start Ollama manually
ollama serve &

# 4. Start the app services
docker compose -f docker-compose.yml -f docker-compose.mac.yml up --build
```

App services connect to Ollama via the OpenAI-compatible API (`/v1`):

| Platform | `OLLAMA_BASE_URL` |
|---|---|
| Mac (native) | `http://localhost:11434/v1` (default) |
| Linux/Windows (Docker) | `http://ollama:11434/v1` |
| Mac (Docker app → host Ollama) | `http://host.docker.internal:11434/v1` |
