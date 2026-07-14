package org.learnings.ai.shoppingassistant.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

@Component
public class SupportAgent implements Agent {

    private final ChatClient chatClient;

    public SupportAgent(ChatClient.Builder chatClientBuilderWithChatMemory, QuestionAnswerAdvisor ragAdvisor) {
        this.chatClient = chatClientBuilderWithChatMemory
                .defaultAdvisors(ragAdvisor)
                .build();
    }

    @Override
    public ChatResponse chat(String message, String convId) {
        return null;
    }

    @Override
    public String name() {
        return "";
    }
}
