package org.learnings.ai.shoppingassistant.config;

import org.learnings.ai.shoppingassistant.agents.Agent;
import org.learnings.ai.shoppingassistant.agents.ChatClientAgent;
import org.learnings.ai.shoppingassistant.agents.prompts.PromptProvider;
import org.learnings.ai.shoppingassistant.tools.OrderAgentTool;
import org.learnings.ai.shoppingassistant.tools.ShoppingAgentTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AgentsConfig {

    @Bean
    Agent shoppingAgent(ChatClient.Builder builder, PromptProvider shoppingPromptProvider,
                        List<ShoppingAgentTool> tools) {
        return new ChatClientAgent("shopping", builder.build(), shoppingPromptProvider, tools);
    }

    @Bean
    Agent orderAgent(ChatClient.Builder builder, PromptProvider orderPromptProvider, List<OrderAgentTool> tools) {
        return new ChatClientAgent("orders", builder.build(), orderPromptProvider, tools);
    }

    @Bean
    Agent supportAgent(ChatClient.Builder builder, QuestionAnswerAdvisor ragAdvisor,
                       PromptProvider supportPromptProvider) {
        return new ChatClientAgent("support", builder.defaultAdvisors(ragAdvisor).build(),
                supportPromptProvider, List.of());
    }
}
