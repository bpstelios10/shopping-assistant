# Spring AI Evaluations

The evaluation tests are used to **test the decisions made by the LLMs and evaluate prompts/models for regressions**. They are especially useful when changing prompts or models, allowing the same test cases to be run again and compared.

Each agent has **one parameterized JUnit test**, with the evaluation cases defined in JSON files under `src/test/resources/evaluation/`. The test loads all cases, invokes the agent, evaluates the result, logs the evaluation, and asserts that it passes.

## SupportAgent

The `SupportAgent` uses RAG, so its evaluation focuses on the generated answer:

* `RelevancyEvaluator` — checks whether the answer is relevant to the user's question.
* `FactCheckingEvaluator` — checks whether the answer is factually supported by the provided context/documents.

These use an LLM evaluator because answer relevance and factual correctness are semantic properties that are difficult to verify with simple assertions.

## RouterAgent

The `RouterAgent` produces a `RoutingPlan` containing one or more routing decisions (`agent`, `task`, and `confidence`).

A custom `RoutingEvaluator` is used instead of an LLM-based evaluator. The expected routing plan is defined in the JSON test case, and the evaluator performs deterministic checks:

* The number of decisions is the same.
* The selected agents are the same.
* The generated task contains the expected keywords/fragments.
* The confidence is above the required minimum.
* All checks are performed so the evaluation feedback reports every failure, rather than stopping at the first one.

This avoids an additional LLM call and makes router evaluation deterministic and reproducible.

## Evaluation cases

Evaluation cases are stored separately as JSON, for example:

`src/test/resources/evaluation/router/router-cases.json`

This keeps the test logic separate from the scenarios being evaluated and makes it easy to add cases or rerun the same evaluation suite after changing a prompt or model.

## Evaluation approach

Different parts of the system use different evaluation strategies:

* **RAG answer quality:** LLM-based evaluation (`RelevancyEvaluator`, `FactCheckingEvaluator`)
* **Router decisions:** deterministic custom `RoutingEvaluator`
* **Prompt/model regression:** run the same parameterized evaluation suite and compare the results

The goal is to evaluate the **behavior and decisions of the LLMs**, rather than simply testing that the Java code executes successfully.
