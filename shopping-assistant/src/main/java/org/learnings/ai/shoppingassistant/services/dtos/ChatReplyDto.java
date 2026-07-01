package org.learnings.ai.shoppingassistant.services.dtos;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;

import java.util.List;

public record ChatReplyDto(String model, Integer promptTokens, Integer completionTokens,
                           List<GenerationDto> generations) {

    public record GenerationDto(List<AssistantMessage.ToolCall> toolCalls, String text, MessageType messageType,
                                String reasoningContent) {
    }
}
