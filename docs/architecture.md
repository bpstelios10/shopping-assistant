# Multi-Agent Architecture

## Goal

Split responsibilities into specialized agents instead of having one large "God Agent".

Benefits:

* Better separation of concerns
* Easier to maintain
* Easier to scale
* Independent prompts, tools and RAG sources
* New agents can be added without affecting existing ones

---

## Orchestrator

**Responsibility**

Route the user request to the appropriate agent.

**Knows**

* Available agents
* Routing logic

**Does NOT know**

* Business logic
* Tools
* RAG

---

## Shopping Agent

**Responsibility**

Help users discover and choose products.

**Owns**

* Product search
* Product recommendations
* Product comparisons

**Uses**

* Products Tool

---

## Order Agent

**Responsibility**

Manage customer orders.

**Owns**

* Place order
* Cancel order
* Order status
* Order history

**Uses**

* Orders Tool

---

## Support Agent

**Responsibility**

Answer customer support questions.

**Owns**

* Refund policy
* Shipping
* Warranty
* FAQs

**Uses**

* RAG

---

## Shared Infrastructure

Shared by all agents:

* Chat Memory
* User Memory
* Conversation Summarization
* Observability
* LLM
* Embedding Model

---

## Routing

Current: `RouterAgent` (LLM classifier) returns a `RoutingDecision {agent, confidence}`.
`AgentOrchestrator` dispatches to the chosen agent; below a confidence threshold it falls
back to the shopping agent. The router uses its own memory-free `ChatClient`.

## Future Evolution

Evolve single-agent routing into a multi-agent **planner** that can pick (or combine)
several agents per request.

```text
User
   │
   ▼
Planner / Orchestrator
   ├── Shopping Agent
   ├── Order Agent
   └── Support Agent
```

The planner decides which agent (or agents) should execute the user's request.
