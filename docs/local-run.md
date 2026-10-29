# Local Run Guide

## Setup

Runtime requirements:

- Java `25` (project source/target compatibility)
- Docker + Docker Compose

The app requires **PostgreSQL with pgvector** and **Redis Stack** running before startup.
Shopping and order flows also need the external Products (`:8082`) and Orders (`:8083`) services.

Flyway migrations run automatically on startup against the configured datasource
(`spring.datasource.*`), including the `user_memory` table migration.

## Run

### 1) Start dependencies

```shell
# Linux/Windows
docker compose up postgres redis -d

# Mac
docker compose -f docker-compose.yml -f docker-compose.mac.yml up postgres redis -d
```

The container uses `restart: unless-stopped`, so this is usually one-time per machine.

### 2) Start the model runtime

#### Linux / Windows (Ollama in Docker)

```shell
docker compose up -d ollama ollama-init
```

Ollama and the models (`qwen3:8b`, `nomic-embed-text`, `qwen2.5:3b-instruct`) are managed in Docker.
Models are stored in a named volume and downloaded once by the `ollama-init` service.

#### Mac (Apple Silicon, native Ollama)

Running Ollama in Docker on macOS is CPU-only. For Metal/GPU acceleration, run Ollama natively:

```shell
# 1. Install Ollama natively (once)
#    Match the version in OLLAMA_VERSION in .env
#    Releases: https://github.com/ollama/ollama/releases
brew install ollama
brew services stop ollama

# 2. Pull the models (once)
ollama serve &
ollama pull qwen3:8b
ollama pull nomic-embed-text
ollama pull qwen2.5:3b-instruct   # optional, not used by the app yet
pkill ollama

# 3. Start Ollama before the app
ollama serve &
```

### 3) Start the application

The application itself is not part of `docker-compose.yml`; run it from the Gradle project:

```shell
cd shopping-assistant
./gradlew bootRun
```

App services connect to Ollama through the OpenAI-compatible API (`/v1`):

| Platform | `OLLAMA_BASE_URL` |
|---|---|
| Mac (native) | `http://localhost:11434/v1` (default) |
| Linux/Windows (Ollama in Docker, app on host) | `http://localhost:11434/v1` (default, port is published) |
| App in the same Docker network as Ollama | `http://ollama:11434/v1` |
| Mac (Docker app -> host Ollama) | `http://host.docker.internal:11434/v1` |

## Verify

Check app endpoint:

```shell
curl -X POST http://localhost:8080/shopping-assistant/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"I need running shoes under 120","conversationId":"demo-1"}'
```

Check management endpoints:

```shell
curl http://localhost:8081/shopping-assistant/private/health
curl http://localhost:8081/shopping-assistant/private/metrics
```

## Troubleshooting

- If startup fails on DB/Redis connection, verify containers are up and host/port env vars are correct.
- If responses are slow or missing on macOS, confirm native Ollama is running (`ollama serve`).
- If model pull fails, retry `ollama pull qwen3:8b` and confirm internet access.
