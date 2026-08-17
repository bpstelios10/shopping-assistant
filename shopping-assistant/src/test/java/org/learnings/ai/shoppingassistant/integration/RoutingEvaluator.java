package org.learnings.ai.shoppingassistant.integration;

import org.jspecify.annotations.NonNull;
import org.learnings.ai.shoppingassistant.agents.RouterAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;

public class RoutingEvaluator implements Evaluator {

    private static final Logger log = LoggerFactory.getLogger(RoutingEvaluator.class);
    private static final double MIN_CONFIDENCE = 0.9;

    private final ObjectMapper objectMapper;
    private final RoutingEvaluatorPlan expectedPlan;

    public RoutingEvaluator(ObjectMapper objectMapper, RoutingEvaluatorPlan expectedPlan) {
        this.objectMapper = objectMapper;
        this.expectedPlan = expectedPlan;
    }

    @Override
    public @NonNull EvaluationResponse evaluate(EvaluationRequest request) {
        RouterAgent.RoutingPlan actualPlan = parseAction(request.getResponseContent());

        if (actualPlan.decisions().size() != expectedPlan.decisions().size()) {
            return getFailingEvaluationResponse(String.format("""
                    Expected decisions:
                    %s
                    but got:
                    %s""", expectedPlan.decisions(), actualPlan.decisions()));
        }

        for (int i = 0; i < actualPlan.decisions().size(); i++) {
            RoutingEvaluatorStep expectedStep = expectedPlan.decisions().get(i);
            RouterAgent.RoutingStep actualStep = actualPlan.decisions().get(i);

            if (expectedStep.agent() != actualStep.agent()) {
                return getFailingEvaluationResponse(
                        String.format("Decision [%d]: Expected agent: [%s], actual agent: [%s]",
                                i, expectedStep.agent(), actualStep.agent()));
            }

            List<String> missing = expectedStep.taskContains()
                    .stream()
                    .filter(term -> !actualStep.task().contains(term))
                    .toList();

            if (!missing.isEmpty()) {
                return getFailingEvaluationResponse(
                        String.format("Decision [%d]: Tasks missing: [%s]. Actual task: [%s]",
                                i, missing, actualStep.task()));
            }

            if (actualStep.confidence() < MIN_CONFIDENCE) {
                return getFailingEvaluationResponse(
                        String.format("Decision [%d]: Confidence: [%f] is below minimum: [%f]",
                                i, actualStep.confidence(), MIN_CONFIDENCE));
            }
        }

        return new EvaluationResponse(true, 1f, "Correct routing decision", new HashMap<>());
    }

    public record RoutingEvaluatorPlan(List<RoutingEvaluatorStep> decisions) {
    }

    public record RoutingEvaluatorStep(RouterAgent.RoutingStep.AgentType agent, List<String> taskContains) {
    }

    private RouterAgent.RoutingPlan parseAction(String responseContent) {
        try {
            return objectMapper.readValue(responseContent, RouterAgent.RoutingPlan.class);
        } catch (Exception e) {
            log.error("Failed to read the RoutingPlan from router response [{}].", responseContent, e);
            throw new RuntimeException(e);
        }
    }

    private @NonNull EvaluationResponse getFailingEvaluationResponse(String feedback) {
        return new EvaluationResponse(false, 0f, feedback, new HashMap<>());
    }
}
