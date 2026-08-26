package org.learnings.ai.shoppingassistant.integration.evaluation.cases;

import org.learnings.ai.shoppingassistant.integration.evaluation.evaluators.ToolCallingEvaluator;

import java.util.List;

public record EvaluationCase(
        String id,
        String question,
        List<String> expectedDocuments,
        List<ToolCallingEvaluator.EvaluationTool> expectedTools) {
}
