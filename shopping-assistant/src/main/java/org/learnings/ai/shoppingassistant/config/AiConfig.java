package org.learnings.ai.shoppingassistant.config;

import org.learnings.ai.shoppingassistant.services.memory.SummaryBufferChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    ChatMemory chatMemory(RedisChatMemoryRepository chatMemoryRepository, ChatModel chatModel) {
        return new SummaryBufferChatMemory(chatMemoryRepository, chatModel, 10, 20);
    }

    @Bean
    ChatClient chatClient(ChatModel chatModel, MessageChatMemoryAdvisor memoryAdvisor, QuestionAnswerAdvisor ragAdvisor) {
        return ChatClient.builder(chatModel)
                // advisors go here for now, but could be part of the agent, if we separate agents later
                .defaultAdvisors(memoryAdvisor, ragAdvisor)
                .build();
    }

    @Bean
    MessageChatMemoryAdvisor memoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor
                .builder(chatMemory)
                .order(1) // runs first: stores/reads RAW user message
                .build();
    }

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
