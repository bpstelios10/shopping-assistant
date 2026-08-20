package org.learnings.ai.shoppingassistant.integration;

import java.util.List;

public record FunctionalAgentEvaluationCase(
        String id, String question, List<ToolCallingEvaluator.EvaluationTool> expectedTools) {
}
