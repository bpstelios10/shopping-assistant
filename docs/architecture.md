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

## Future Evolution

Replace the routing logic with an LLM-powered planner.

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
