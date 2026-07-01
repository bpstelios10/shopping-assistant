package org.learnings.ai.shoppingassistant.services;

import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatReplyDto chat(String message) {
        ChatResponse agentResponse = chatClient
                .prompt()
                .user(message)
                .call()
                .chatResponse();

        if (agentResponse == null) {
            throw new RuntimeException("Agent didnt reply");
        }

        List<ChatReplyDto.GenerationDto> generations = agentResponse.getResults().stream()
                .map(result -> new ChatReplyDto.GenerationDto(
                        result.getOutput().getToolCalls(),
                        result.getOutput().getText(),
                        (MessageType) result.getOutput().getMetadata().get("messageType"),
                        (String) result.getOutput().getMetadata().get("reasoningContent")
                ))
                .toList();
        ChatResponseMetadata metadata = agentResponse.getMetadata();

        return new ChatReplyDto(
                metadata.getModel(),
                metadata.getUsage().getPromptTokens(),
                metadata.getUsage().getCompletionTokens(),
                generations
        );
    }
}
