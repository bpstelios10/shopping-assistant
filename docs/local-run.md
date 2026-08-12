# Local Run Guide

## Setup

Runtime requirements:

- Java `25` (project source/target compatibility)
- Docker + Docker Compose

The app requires **PostgreSQL with pgvector** and **Redis Stack** running before startup.

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

### 2) Start the app stack

#### Linux / Windows (Ollama in Docker)

```shell
docker compose up --build
```

Ollama and `qwen3:8b` are managed in Docker. The model is stored in a named volume and is downloaded once.

#### Mac (Apple Silicon, native Ollama)

Running Ollama in Docker on macOS is CPU-only. For Metal/GPU acceleration, run Ollama natively:

```shell
# 1. Install Ollama natively (once)
#    Match the version in OLLAMA_VERSION in .env
#    Releases: https://github.com/ollama/ollama/releases
brew install ollama
brew services stop ollama

# 2. Pull the model (once)
ollama serve &
ollama pull qwen3:8b
pkill ollama

# 3. Start Ollama before the app
ollama serve &

# 4. Start app services
docker compose -f docker-compose.yml -f docker-compose.mac.yml up --build
```

App services connect to Ollama through the OpenAI-compatible API (`/v1`):

| Platform | `OLLAMA_BASE_URL` |
|---|---|
| Mac (native) | `http://localhost:11434/v1` (default) |
| Linux/Windows (Docker) | `http://ollama:11434/v1` |
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
