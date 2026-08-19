package org.learnings.ai.shoppingassistant.agents;

import lombok.extern.slf4j.Slf4j;
import org.learnings.ai.shoppingassistant.agents.prompts.PromptProvider;
import org.learnings.ai.shoppingassistant.tools.ShoppingAgentTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ShoppingAgent implements Agent {

    private final ChatClient chatClient;
    private final PromptProvider shoppingPromptProvider;
    private final List<ShoppingAgentTool> tools;

    public ShoppingAgent(ChatClient.Builder chatClientBuilderWithChatMemory, PromptProvider shoppingPromptProvider,
                         List<ShoppingAgentTool> tools) {
        this.chatClient = chatClientBuilderWithChatMemory.build();
        this.shoppingPromptProvider = shoppingPromptProvider;
        this.tools = tools;
    }

    @Override
    public AgentChatResult chat(String message, String conversationId) {
        ChatClient.CallResponseSpec responseSpec = chatClient
                .prompt(shoppingPromptProvider.buildPrompt(message))
                .advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, conversationId)
                        .param("agent", name()))
                .tools(tools.toArray())
                .call();

        ChatClientResponse chatClientResponse = responseSpec.chatClientResponse();

        if (chatClientResponse.chatResponse() == null) {
            throw new RuntimeException("Agent didnt reply");
        }

        return AgentChatResult.from(chatClientResponse);
    }

    @Override
    public String name() {
        return "shopping";
    }
}
