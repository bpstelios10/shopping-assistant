package org.learnings.ai.shoppingassistant.services;

import org.learnings.ai.shoppingassistant.agents.AgentChatResult;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

public final class ChatReplyMapper {

    private static final String REASONING_CONTENT = "reasoningContent";

    private ChatReplyMapper() {
    }

    public static ChatReplyDto toChatReplyDto(AgentChatResult agentChatResponse, String conversationId) {
        ChatResponse chatResponse = agentChatResponse.chatResponse();
        ChatResponseMetadata metadata = chatResponse.getMetadata();
        List<AssistantMessage.ToolCall> toolCalls = agentChatResponse.toolCalls();
        List<ChatReplyDto.ToolCall> toolsCalled = toolCalls == null ? List.of() :
                toolCalls.stream()
                        .map(ChatReplyMapper::toToolCall)
                        .toList();

        return new ChatReplyDto(
                metadata.getModel(),
                conversationId,
                metadata.getUsage().getPromptTokens(),
                metadata.getUsage().getCompletionTokens(),
                toolsCalled,
                chatResponse.getResults().stream()
                        .map(ChatReplyMapper::toGenerationDto)
                        .toList()
        );
    }

    private static ChatReplyDto.GenerationDto toGenerationDto(Generation generation) {
        AssistantMessage output = generation.getOutput();
        return new ChatReplyDto.GenerationDto(
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
