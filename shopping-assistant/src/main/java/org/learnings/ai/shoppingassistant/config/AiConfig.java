package org.learnings.ai.shoppingassistant.config;

import org.learnings.ai.shoppingassistant.services.memory.SummaryBufferChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.chat.memory.redis.autoconfigure.RedisChatMemoryProperties;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.util.StringUtils;
import redis.clients.jedis.RedisClient;

@Configuration
public class AiConfig {

    @Bean
    RedisChatMemoryRepository redisChatMemoryRepository(RedisClient jedisClient, RedisChatMemoryProperties properties) {
        RedisChatMemoryRepository.Builder builder = RedisChatMemoryRepository.builder().jedisClient(jedisClient);

        if (StringUtils.hasText(properties.getIndexName())) {
            builder.indexName(properties.getIndexName());
        }
        if (StringUtils.hasText(properties.getKeyPrefix())) {
            builder.keyPrefix(properties.getKeyPrefix());
        }
        if (properties.getTimeToLive() != null && properties.getTimeToLive().toSeconds() > 0) {
            builder.timeToLive(properties.getTimeToLive());
        }
        builder.initializeSchema(properties.getInitializeSchema());

        return builder.build();
    }

    @Bean
    ChatMemory chatMemory(RedisChatMemoryRepository chatMemoryRepository, ChatModel chatModel) {
        return new SummaryBufferChatMemory(chatMemoryRepository, chatModel, 10, 20);
    }

    @Bean
    MessageChatMemoryAdvisor memoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor
                .builder(chatMemory)
                .order(1) // runs first: stores/reads RAW user message
                .build();
    }

    @Bean
    @Scope("prototype") // builder beans are singletons. make it prototype to avoid leaks
    ChatClient.Builder chatClientBuilderWithChatMemory(ChatModel chatModel, MessageChatMemoryAdvisor memoryAdvisor) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(memoryAdvisor);
    }

    @Bean
    ChatClient routerChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    // TODO make this a tool. toll is better for individual cases. advisors common for ALL agents (like chat memory)
    @Bean
    QuestionAnswerAdvisor ragAdvisor(VectorStore vectorStore) {
        // this is a guard, for 'dumb' models like qwen8. weak instruction-following makes it merge context sometimes
        PromptTemplate qaTemplate = PromptTemplate.builder()
                .template("""
                        Answer ONLY the user's latest question below.
                        Use the context if relevant; ignore it if not.
                        Do not answer previous questions.
                        
                        Context:
                        ---------------------
                        {question_answer_context}
                        ---------------------
                        
                        Latest question: {query}
                        """)
                .build();

        return QuestionAnswerAdvisor.builder(vectorStore)
                .order(2) // runs after: augments only THIS request, not memory
                .promptTemplate(qaTemplate)
                .searchRequest(SearchRequest.builder()
                        .topK(4)
                        .similarityThreshold(0.5)
                        .build())
                .build();
    }
}
