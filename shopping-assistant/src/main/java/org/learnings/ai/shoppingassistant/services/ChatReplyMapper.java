package org.learnings.ai.shoppingassistant.services;

import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

final class ChatReplyMapper {

    private static final String REASONING_CONTENT = "reasoningContent";

    private ChatReplyMapper() {
    }

    static ChatReplyDto toChatReplyDto(ChatResponse response) {
        ChatResponseMetadata metadata = response.getMetadata();
        return new ChatReplyDto(
                metadata.getModel(),
                metadata.getUsage().getPromptTokens(),
                metadata.getUsage().getCompletionTokens(),
                response.getResults().stream()
                        .map(ChatReplyMapper::toGenerationDto)
                        .toList()
        );
    }

    private static ChatReplyDto.GenerationDto toGenerationDto(Generation generation) {
        AssistantMessage output = generation.getOutput();
        return new ChatReplyDto.GenerationDto(
                output.getToolCalls().stream()
                        .map(ChatReplyMapper::toToolCall)
                        .toList(),
                output.getText(),
                output.getMessageType().getValue(),
                (String) output.getMetadata().get(REASONING_CONTENT)
        );
    }

    private static ChatReplyDto.ToolCall toToolCall(AssistantMessage.ToolCall toolCall) {
        return new ChatReplyDto.ToolCall(
                toolCall.id(),
                toolCall.type(),
                toolCall.name(),
                toolCall.arguments()
        );
    }
}
