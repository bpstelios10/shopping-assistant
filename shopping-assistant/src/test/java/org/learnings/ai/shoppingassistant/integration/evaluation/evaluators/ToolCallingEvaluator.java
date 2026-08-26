package org.learnings.ai.shoppingassistant.integration.evaluation.evaluators;

import org.jspecify.annotations.NonNull;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluates the tool calls made. it receives the expected (required) tool calls and asserts on the actual ones.
 * If all the required tool calls are there, then it succeeds.
 * The non-required tools are obviously some that we cant predict.
 * The score of the evaluation is 1 minus 0.1 * non-required tool calls.
 */
public class ToolCallingEvaluator implements Evaluator {

    private static final Logger log = LoggerFactory.getLogger(ToolCallingEvaluator.class);

    private final ObjectMapper objectMapper;
    private final List<EvaluationTool> expectedTools;

    public ToolCallingEvaluator(ObjectMapper objectMapper, List<EvaluationTool> expectedTools) {
        this.objectMapper = objectMapper;
        this.expectedTools = expectedTools;
    }

    @Override
    public @NonNull EvaluationResponse evaluate(EvaluationRequest request) {
        ChatReplyDto chatReplyDto = parseAction(request.getResponseContent());

        if (chatReplyDto.toolsCalled().size() < expectedTools.size()) {
            return getFailingEvaluationResponse(String.format("""
                    
                    Expected tool calls are more than the actual calls.
                    Expected: %s
                    but got: %s""", expectedTools, chatReplyDto.toolsCalled()));
        }

        List<EvaluationTool> actualToolCalls = chatReplyDto.toolsCalled().stream()
                .map(tc -> new EvaluationTool(tc.name(), tc.arguments()))
                .collect(Collectors.toList());

        for (EvaluationTool tool: expectedTools) {
            if (!actualToolCalls.contains(tool)) {
                return getFailingEvaluationResponse(String.format("""
                    
                    Tool call not found: %s
                    Actual tools: %s""", tool, chatReplyDto.toolsCalled()));
            }

            actualToolCalls.remove(tool);
        }

        float score = 1f - actualToolCalls.size() / 10f;

        return new EvaluationResponse(true, score, "Correct called tools", new HashMap<>());
    }

    // TODO add a param to say required or optional tool calls or something?
    public record EvaluationTool(String name, String arguments) {
    }

    private ChatReplyDto parseAction(String responseContent) {
        try {
            return objectMapper.readValue(responseContent, ChatReplyDto.class);
        } catch (Exception e) {
            log.error("Failed to read the AgentChatResult from agent response [{}].", responseContent, e);
            throw new RuntimeException(e);
        }
    }

    private @NonNull EvaluationResponse getFailingEvaluationResponse(String feedback) {
        return new EvaluationResponse(false, 0f, feedback, new HashMap<>());
    }
}
