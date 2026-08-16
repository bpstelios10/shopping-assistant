package org.learnings.ai.shoppingassistant.integration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class EvaluationConfig {

    @Bean
    ChatClient.Builder evaluationChatClientBuilder(ChatModel chatModel) {
        ChatOptions.Builder<?> evaluationChatOptionsBuilder = ChatOptions.builder()
                .model("qwen2.5:3b-instruct")
                .temperature(0.0)
                .maxTokens(100);

        return ChatClient
                .builder(chatModel)
                .defaultOptions(evaluationChatOptionsBuilder);
    }

    @Bean
    public RelevancyEvaluator relevancyEvaluator(ChatClient.Builder evaluationChatClientBuilder) {
        return RelevancyEvaluator.builder().chatClientBuilder(evaluationChatClientBuilder).build();
    }

    @Bean
    public FactCheckingEvaluator factCheckingEvaluator(ChatClient.Builder evaluationChatClientBuilder) {
        return FactCheckingEvaluator.builder(evaluationChatClientBuilder).build();
    }
}
