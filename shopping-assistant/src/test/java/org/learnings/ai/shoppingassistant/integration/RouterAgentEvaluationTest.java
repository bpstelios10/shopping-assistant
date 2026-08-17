package org.learnings.ai.shoppingassistant.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.learnings.ai.shoppingassistant.agents.RouterAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Tag("integration")
@SpringBootTest
@Import({EvaluationConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RouterAgentEvaluationTest {

    private static final Logger log = LoggerFactory.getLogger(RouterAgentEvaluationTest.class);

    @Autowired
    private RouterAgent routerAgent;
    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest
    @MethodSource("loadCases")
    void supportAgent_whenAskedAboutRefundsOrShippingOrFaq_shouldPassTheEvaluation(RoutingEvaluationCase testCase) {
        RoutingEvaluator routingEvaluator = new RoutingEvaluator(objectMapper, testCase.expectedPlan());

        RouterAgent.RoutingPlan routingResponse = routerAgent.route(testCase.question());

        assertThat(routingResponse).isNotNull();
        List<RouterAgent.RoutingStep> decisions = routingResponse.decisions();
        log.debug("Decisions: {}.", decisions);
        assertThat(decisions).isNotEmpty();

        EvaluationRequest evaluationRequest =
                new EvaluationRequest(testCase.question(), objectMapper.writeValueAsString(routingResponse));
        EvaluationResponse relevance = routingEvaluator.evaluate(evaluationRequest);
        // PERFORMANCE TESTING: FOR CHANGES OF PROMPTS OR MODELS. THEN COMPARE BEFORE AND AFTER
        log.warn("Test: {} - Relevance: pass={}, score={}, feedback={}.",
                testCase.id(), relevance.isPass(), relevance.getScore(), relevance.getFeedback());

        assertThat(relevance.isPass())
                .as("Answer should be relevant")
                .isTrue();
        assertThat(relevance.getScore()).isGreaterThan(0.9f);
    }

    private Stream<Arguments> loadCases() throws IOException {
        var resource = new ClassPathResource("evaluation/router/router-cases.json");

        List<RoutingEvaluationCase> cases = objectMapper.readValue(resource.getInputStream(), new TypeReference<>() { });

        return cases.stream().map(c -> arguments(named(c.id(), c)));
    }
}
