package org.learnings.ai.shoppingassistant.services.dtos;

import java.util.List;

public record ChatReplyDto(String model, Integer promptTokens, Integer completionTokens,
                           List<GenerationDto> generations) {

    // TODO toolCalls will always be empty cause the tool is called in the final step so not shown here
    public record GenerationDto(List<ToolCall> toolCalls, String text, String messageType, String reasoningContent) {
    }

    public record ToolCall(String id, String type, String name, String arguments) {
    }
}
