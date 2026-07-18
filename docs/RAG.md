# RAG

## What exists so far

- RAG ingestion is implemented in `shopping-assistant/src/main/java/org/learnings/ai/shoppingassistant/rag/loaddocs`.
- `RagIngestionRunner` is a standalone Spring Boot entrypoint for document loading.
- It runs with `WebApplicationType.NONE` and uses `DocumentImporter` to load files into `vector_store`.
- It is for manual ingestion and is not part of the normal runtime flow of `ShoppingAssistantApplication`.

## Loader behavior

- Current strategy is simple: wipe `vector_store`, then re-import all files from the provided folder.
- Reader: `TikaDocumentReader`.
- Chunking: `TokenTextSplitter` (chunk size 500).
- Metadata added per chunk: `filename`.

## Run the loader

Simply use the IntelliJ runners. Or in command line:

```shell
./gradlew :shopping-assistant:classes
java -cp shopping-assistant/build/classes/java/main:shopping-assistant/build/resources/main org.learnings.ai.shoppingassistant.rag.loaddocs.RagIngestionRunner <path-to-folder>
```

## DB cheat sheet

Assuming Postgres/pgvector is running in Docker:

```shell
# get into the container for user shopping_assistant
docker exec -it <container-name> psql -U shopping_assistant

# list databases
\l

# connect to the shopping_assistant database
\c shopping_assistant

# list tables
\dt

# check number of stored chunks
SELECT count(*) FROM vector_store;

# sample stored content
SELECT id, content FROM vector_store LIMIT 3;
```
