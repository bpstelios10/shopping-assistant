package org.learnings.ai.shoppingassistant.services;

import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.learnings.ai.shoppingassistant.tools.ProductTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class AgentServiceImpl implements AgentService {

    private final ChatClient chatClient;
    private final PromptService promptService;
    private final ProductTools productTools;

    public AgentServiceImpl(ChatClient chatClient, PromptService promptService, ProductTools productTools) {
        this.chatClient = chatClient;
        this.promptService = promptService;
        this.productTools = productTools;
    }

    @Override
    public ChatReplyDto chat(String message) {
        ChatResponse chatResponse = chatClient
                .prompt(promptService.buildShoppingAssistantPrompt(message))
                .tools(productTools)
                .call()
                .chatResponse();

        if (chatResponse == null) {
            throw new RuntimeException("Agent didnt reply");
        }

        return ChatReplyMapper.toChatReplyDto(chatResponse);
    }
}
