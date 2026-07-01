package org.learnings.ai.shoppingassistant.services;

import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final PromptService promptService;

    public ChatServiceImpl(ChatClient chatClient, PromptService promptService) {
        this.chatClient = chatClient;
        this.promptService = promptService;
    }

    @Override
    public ChatReplyDto chat(String message) {
        ChatResponse agentResponse = chatClient
                .prompt(promptService.buildShoppingAssistantPrompt(message))
                .call()
                .chatResponse();

        if (agentResponse == null) {
            throw new RuntimeException("Agent didnt reply");
        }

        return ChatReplyMapper.toChatReplyDto(agentResponse);
    }
}
