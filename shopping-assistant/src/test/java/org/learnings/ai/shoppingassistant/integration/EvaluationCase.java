package org.learnings.ai.shoppingassistant.integration;

import java.util.List;

public record EvaluationCase(String id, String question, List<String> expectedDocuments, List<String> expectedTools) {
}
