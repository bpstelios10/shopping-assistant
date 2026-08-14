# Observability & Metrics

The application uses Spring Boot, Spring AI and custom application metrics to monitor the AI pipeline.

## LLM

`gen_ai_client_*`

Provided by the Spring AI/OpenAI observability integration.

Tracks:

* LLM call count
* Latency
* Errors
* Input/output/total tokens

Grouped by operation such as:

* `chat`
* `embeddings`

## Agents

`spring_ai_chat_client_*`

Tracks ChatClient execution per agent.

Labels:

* `agent=router`
* `agent=shopping`
* `agent=orders`
* `agent=support`

Tracks:

* Execution count
* Latency

## Tools

`spring_ai_tool_*`

Tracks tool execution.

Tracks:

* Invocation count
* Latency
* Errors

Useful for identifying slow or frequently-used tools.

## Advisors

`spring_ai_advisor_*`

Tracks advisor execution.

Labels identify the advisor being executed.

Tracks:

* Execution count
* Latency

## RAG / Vector Store

`db_vector_client_operation_*`

Tracks vector-store operations against pgvector.

Tracks:

* Vector operation count
* Latency

Useful for monitoring RAG retrieval performance.

## User Memory

`jdbc_query_*`

Provided by the JDBC instrumentation.

Tracks database queries used by user-memory persistence.

## Conversation Memory

`chat_memory_*`

Custom application metric for Redis-backed conversation memory.

Tracks:

* Memory operations
* Operation latency

## Infrastructure

Spring Boot provides additional metrics for:

* HTTP requests
* JVM
* HikariCP
* JDBC
* Executors
* Disk

## Future Metrics

Potential additions:

* Router result / selected agent
* Routing confidence
* RAG retrieval quality
* Tool success/failure breakdown
* Token/cost metrics per agent
* Distributed tracing across agents, tools and downstream services

The goal is to monitor the complete AI request path:

HTTP → Router → Agent → Advisor → LLM → Tool/RAG/Memory → Response
