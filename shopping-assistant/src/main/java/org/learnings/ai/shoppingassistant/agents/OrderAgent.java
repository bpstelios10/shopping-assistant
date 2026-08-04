package org.learnings.ai.shoppingassistant.agents;

import lombok.extern.slf4j.Slf4j;
import org.learnings.ai.shoppingassistant.agents.prompts.PromptProvider;
import org.learnings.ai.shoppingassistant.tools.AgentTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class OrderAgent implements Agent {

    private final ChatClient chatClient;
    private final PromptProvider orderPromptProvider;
    private final List<AgentTool> tools;

    public OrderAgent(ChatClient.Builder chatClientBuilderWithChatMemory, PromptProvider orderPromptProvider,
                      List<AgentTool> tools) {
        this.chatClient = chatClientBuilderWithChatMemory.build();
        this.orderPromptProvider = orderPromptProvider;
        this.tools = tools;
    }

    @Override
    public ChatResponse chat(String message, String conversationId) {
        ChatResponse chatResponse = chatClient
                .prompt(orderPromptProvider.buildPrompt(message))
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
        return "orders";
    }
}
