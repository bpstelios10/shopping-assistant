package org.learnings.ai.shoppingassistant.integration;

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

        if (chatReplyDto.toolsCalled().size() != expectedTools.size()) {
            return getFailingEvaluationResponse(String.format("""
                    Expected tool calls:
                    %s
                    but got:
                    %s""", expectedTools, chatReplyDto.toolsCalled()));
        }

        for (int i = 0; i < chatReplyDto.toolsCalled().size(); i++) {
            ChatReplyDto.ToolCall actual = chatReplyDto.toolsCalled().get(i);
            EvaluationTool evaluationTool = expectedTools.get(i);

            if (!evaluationTool.name().equals(actual.name())) {
                return getFailingEvaluationResponse(
                        String.format("ToolCall [%d]: Expected name: [%s], actual name: [%s]",
                                i, evaluationTool.name(), actual.name()));
            }

            if (!evaluationTool.arguments().equals(actual.arguments())) {
                return getFailingEvaluationResponse(
                        String.format("ToolCall [%d]: Expected arguments: [%s], actual arguments: [%s]",
                                i, evaluationTool.arguments(), actual.arguments()));
            }
        }

        return new EvaluationResponse(true, 1f, "Correct called tools", new HashMap<>());
    }

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
