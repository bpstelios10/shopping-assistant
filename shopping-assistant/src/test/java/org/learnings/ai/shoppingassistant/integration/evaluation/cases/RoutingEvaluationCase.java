package org.learnings.ai.shoppingassistant.integration.evaluation.cases;

import org.learnings.ai.shoppingassistant.integration.evaluation.evaluators.RoutingEvaluator;

public record RoutingEvaluationCase(String id, String question, RoutingEvaluator.RoutingEvaluatorPlan expectedPlan) {
}
