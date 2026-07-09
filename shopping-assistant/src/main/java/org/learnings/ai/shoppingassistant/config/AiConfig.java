package org.learnings.ai.shoppingassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    // TODO should migrate from the default in-memory chat-memory to a redis one
    @Bean
    ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                // this goes here for now, but it could be part of the agent, if it a becomes shared one
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
