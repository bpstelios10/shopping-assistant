package org.learnings.ai.shoppingassistant.integration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Map;

@TestConfiguration
public class EvaluationConfig {

    @Bean
    ChatClient.Builder evaluationChatClientBuilder(ChatModel chatModel) {
        ChatOptions.Builder<?> evaluationChatOptionsBuilder = ChatOptions.builder()
//                .model("qwen2.5:3b-instruct") // task get a better model for evaluations
                .temperature(0.0)
                .maxTokens(500)
                .combineWith(
                        OpenAiChatOptions.builder().extraBody(Map.of("think", false))
                );

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
