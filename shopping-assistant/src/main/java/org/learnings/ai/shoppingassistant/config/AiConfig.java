package org.learnings.ai.shoppingassistant.config;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.learnings.ai.shoppingassistant.advisors.ToolCallAuditingAdvisor;
import org.learnings.ai.shoppingassistant.infrastructure.repositories.RedisChatMemoryRepositoryObservationDecorator;
import org.learnings.ai.shoppingassistant.services.memory.SummaryBufferChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.chat.memory.redis.autoconfigure.RedisChatMemoryAutoConfiguration;
import org.springframework.ai.model.chat.memory.redis.autoconfigure.RedisChatMemoryProperties;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import redis.clients.jedis.RedisClient;

import java.util.Map;

@Slf4j
@Configuration
public class AiConfig {

    @Bean
    ToolCallingManager toolCallingManager(ObservationRegistry observationRegistry) {
        return DefaultToolCallingManager.builder()
                .observationRegistry(observationRegistry)
                .build();
    }

    @Bean
    // just delegate it manually to the auto-config, to avoid the conditional exclude of this bean creation
    ChatMemoryRepository redisChatMemoryRepository(RedisClient jedisClient,
                                                   RedisChatMemoryProperties properties,
                                                   RedisChatMemoryAutoConfiguration redisAutoConfig,
                                                   ObservationRegistry observationRegistry) {
        RedisChatMemoryRepository delegate = redisAutoConfig.redisChatMemory(jedisClient, properties);

        return new RedisChatMemoryRepositoryObservationDecorator(delegate, observationRegistry);
    }

    @Bean
    ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository, ChatModel chatModel) {
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
    @Scope("prototype")
    // builder beans are singletons. make it prototype to avoid leaks
    ChatClient.Builder chatClientBuilderWithChatMemory(ChatModel chatModel,
                                                       MessageChatMemoryAdvisor memoryAdvisor,
                                                       ObservationRegistry observationRegistry,
                                                       ToolCallAuditingAdvisor toolCallAuditingAdvisor) {
        return ChatClient
                .builder(chatModel, observationRegistry, null, null)
                .defaultAdvisors(memoryAdvisor, toolCallAuditingAdvisor);
    }

    /**
     * Tools call an external backend that can fail (network, NPE, etc.). By default Spring AI feeds the raw
     * exception text back to the model, which makes it flail and re-open earlier questions. Return a clean,
     * bounded message instead so the model degrades gracefully and stays on the current turn.
     */
    @Bean
    ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        return (ToolExecutionException exception) -> {
            log.warn("tool [{}] failed, returning graceful message to model",
                    exception.getToolDefinition().name(), exception);
            return "This tool is temporarily unavailable. Tell the shopper you couldn't retrieve that "
                    + "information right now and answer only their latest message; do not retry other tools.";
        };
    }

    @Bean
    ChatClient routerChatClient(ChatModel chatModel, ObservationRegistry observationRegistry) {
        ChatOptions.Builder<?> routerChatOptionsBuilder = ChatOptions.builder()
                .temperature(0.0)
                .maxTokens(1000)
                .combineWith(
                        OpenAiChatOptions.builder().extraBody(Map.of("think", false))
                );

        return ChatClient
                .builder(chatModel, observationRegistry, null, null)
                .defaultOptions(routerChatOptionsBuilder)
                .build();
    }

    // thought: could be a tool. tool is better for individual cases. advisors common for ALL agents (like chat memory)
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

    @Bean
    ObservationFilter agentTagFromChatClientContext() {
        return context -> {
            if (context instanceof ChatClientObservationContext c) {
                String agent = (String) c.getRequest().context().get("agent");

                context.addLowCardinalityKeyValue(KeyValue.of(
                        "agent", (agent == null || agent.isBlank()) ? "unknown" : agent));
            }

            return context;
        };
    }
}
