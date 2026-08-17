package org.learnings.ai.shoppingassistant.integration;

public record RoutingEvaluationCase(String id, String question, RoutingEvaluator.RoutingEvaluatorPlan expectedPlan) {
}
