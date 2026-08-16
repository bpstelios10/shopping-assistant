package org.learnings.ai.shoppingassistant.integration;

import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.agents.SupportAgent;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest
@Import({EvaluationConfig.class})
class SupportAgentEvaluationTest {

    private static final Logger log = LoggerFactory.getLogger(SupportAgentEvaluationTest.class);

    @Autowired
    private SupportAgent supportAgent;
    @Autowired
    private RelevancyEvaluator relevancyEvaluator;
    @Autowired
    private FactCheckingEvaluator factCheckingEvaluator;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void refundPolicy() throws Exception {
        EvaluationCase testCase = loadCases().getFirst();

        ChatResponse chatResponse = supportAgent.chat(testCase.question(), "random-conv-id");

        assertThat(chatResponse.getResult()).isNotNull();
        String answer = chatResponse.getResult().getOutput().getText();
        log.debug("Answer: {}.", answer);
        assertThat(answer).isNotEmpty();
        List<Document> documents = chatResponse.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
        assertThat(documents).isNotEmpty();
        assertThat(documents)
                .extracting(document -> document.getMetadata().get("source"))
                .containsExactlyInAnyOrderElementsOf(testCase.expectedDocuments());

        EvaluationRequest evaluationRequest = new EvaluationRequest(testCase.question(), documents, answer);
        EvaluationResponse relevance = relevancyEvaluator.evaluate(evaluationRequest);
        EvaluationResponse groundedness = factCheckingEvaluator.evaluate(evaluationRequest);
        // PERFORMANCE TESTING: FOR CHANGES OF PROMPTS OR MODELS. THEN COMPARE BEFORE AND AFTER
        log.warn("Test: {} - Relevance: pass={}, score={}, feedback={}.",
                testCase.id(), relevance.isPass(), relevance.getScore(), relevance.getFeedback());
        log.warn("Test: {} - Groundedness: pass={}, score={}, feedback={}.",
                testCase.id(), groundedness.isPass(), groundedness.getScore(), groundedness.getFeedback());

        assertThat(relevance.isPass())
                .as("Answer should be relevant")
                .isTrue();
        assertThat(relevance.getScore()).isGreaterThan(0.9f);

        assertThat(groundedness.isPass())
                .as("Answer should be grounded")
                .isTrue();
        assertThat(groundedness.getScore()).isLessThan(0.1f);
    }

    public List<EvaluationCase> loadCases() throws IOException {
        var resource = new ClassPathResource("evaluation/rag/rag-cases.json");

        return objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {
        });
    }
}
