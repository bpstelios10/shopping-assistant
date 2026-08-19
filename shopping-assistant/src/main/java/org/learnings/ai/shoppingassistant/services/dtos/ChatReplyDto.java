package org.learnings.ai.shoppingassistant.services.dtos;

import java.util.List;

// TODO right now the completion tokens only include the last call tokens. i need to include the router as well
public record ChatReplyDto(String model, String conversationId, Integer promptTokens, Integer completionTokens,
                           List<ToolCall> toolsCalled, List<GenerationDto> generations) {

    public record GenerationDto(String text, String messageType, String reasoningContent) {
    }

    public record ToolCall(String id, String type, String name, String arguments) {
    }
}
