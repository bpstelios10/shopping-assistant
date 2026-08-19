package org.learnings.ai.shoppingassistant.agents;

import org.learnings.ai.shoppingassistant.agents.prompts.PromptProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

@Component
public class SupportAgent implements Agent {

    private final ChatClient chatClient;
    private final PromptProvider supportPromptProvider;

    public SupportAgent(ChatClient.Builder chatClientBuilderWithChatMemory, QuestionAnswerAdvisor ragAdvisor,
                        PromptProvider supportPromptProvider) {
        this.chatClient = chatClientBuilderWithChatMemory
                .defaultAdvisors(ragAdvisor)
                .build();
        this.supportPromptProvider = supportPromptProvider;
    }

    @Override
    public AgentChatResult chat(String message, String conversationId) {
        ChatClient.CallResponseSpec responseSpec = chatClient
                .prompt(supportPromptProvider.buildPrompt(message))
                .advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, conversationId)
                        .param("agent", name()))
                .call();

        ChatClientResponse chatClientResponse = responseSpec.chatClientResponse();

        if (chatClientResponse.chatResponse() == null) {
            throw new RuntimeException("Agent didnt reply");
        }

        return AgentChatResult.from(chatClientResponse);
    }

    @Override
    public String name() {
        return "support";
    }
}
