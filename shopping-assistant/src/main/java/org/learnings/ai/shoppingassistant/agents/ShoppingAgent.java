package org.learnings.ai.shoppingassistant.agents;

import lombok.extern.slf4j.Slf4j;
import org.learnings.ai.shoppingassistant.services.PromptService;
import org.learnings.ai.shoppingassistant.services.memory.CurrentUser;
import org.learnings.ai.shoppingassistant.tools.AgentTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ShoppingAgent implements Agent {

    private final ChatClient chatClient;
    private final PromptService promptService;
    private final List<AgentTool> tools;

    public ShoppingAgent(ChatClient.Builder chatClientBuilderWithChatMemory, PromptService promptService,
                         List<AgentTool> tools) {
        this.chatClient = chatClientBuilderWithChatMemory.build();
        this.promptService = promptService;
        this.tools = tools;
    }

    @Override
    public ChatResponse chat(String message, String conversationId) {
        ChatResponse chatResponse = chatClient
                .prompt(promptService.buildShoppingAssistantPrompt(message, CurrentUser.get()))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(tools.toArray())
                .call()
                .chatResponse();

        if (chatResponse == null) {
            throw new RuntimeException("Agent didnt reply");
        }

        return chatResponse;
    }

    @Override
    public String name() {
        return "shopping";
    }
}
