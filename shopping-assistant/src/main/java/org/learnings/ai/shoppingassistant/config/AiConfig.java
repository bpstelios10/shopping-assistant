package org.learnings.ai.shoppingassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    ChatClient chatClient(ChatModel chatModel, MessageChatMemoryAdvisor memoryAdvisor, QuestionAnswerAdvisor ragAdvisor) {
        return ChatClient.builder(chatModel)
                // advisors go here for now, but could be part of the agent, if we separate agents later
                .defaultAdvisors(memoryAdvisor, ragAdvisor)
                .build();
    }

    // TODO should migrate from the default in-memory chat-memory to a redis one
    @Bean
    MessageChatMemoryAdvisor memoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean
    QuestionAnswerAdvisor ragAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(4)
                        .similarityThreshold(0.5)
                        .build())
                .build();
    }
}
