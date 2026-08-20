package org.learnings.ai.shoppingassistant.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.learnings.ai.shoppingassistant.agents.AgentChatResult;
import org.learnings.ai.shoppingassistant.agents.OrderAgent;
import org.learnings.ai.shoppingassistant.services.ChatReplyMapper;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("integration")
@SpringBootTest(properties = {"clients.services.orders.base-url=http://shopping-assistant"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderAgentEvaluationTest {

    private static final Logger log = LoggerFactory.getLogger(OrderAgentEvaluationTest.class);

    @Autowired
    private OrderAgent orderAgent;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private MockRestServiceServer server;

    private String conversationId;

    @AfterEach
    void tearDown() {
        chatMemory.clear(conversationId);
    }

    @ParameterizedTest
    @MethodSource("loadCases")
    void orderAgent_whenAskedAboutRefundsOrShippingOrFaq_shouldPassTheEvaluation(FunctionalAgentEvaluationCase testCase) {
        ToolCallingEvaluator toolCallingEvaluator = new ToolCallingEvaluator(objectMapper, testCase.expectedTools());
        server.reset();
        String orderId = "f47ac10b-58cc-4372-a567-0e02b2c3d002";
        server.expect(requestTo("http://shopping-assistant/orders/" + orderId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id":"%s",
                          "product_id":"prod-123",
                          "quantity":2,
                          "status":"PAID"
                        }
                        """.formatted(orderId), MediaType.APPLICATION_JSON));
        conversationId = "evaluation-" + testCase.id();

        AgentChatResult agentChatResult = orderAgent.chat(testCase.question(), conversationId);

        assertThat(agentChatResult).isNotNull();
        log.debug("AgentChatResult: {}.", agentChatResult);
        ChatReplyDto chatReplyDto = ChatReplyMapper.toChatReplyDto(agentChatResult, conversationId);

        EvaluationRequest evaluationRequest =
                new EvaluationRequest(testCase.question(), objectMapper.writeValueAsString(chatReplyDto));
        EvaluationResponse relevance = toolCallingEvaluator.evaluate(evaluationRequest);
        // PERFORMANCE TESTING: FOR CHANGES OF PROMPTS OR MODELS. THEN COMPARE BEFORE AND AFTER
        log.warn("Test: {} - Relevance: pass={}, score={}, feedback={}.",
                testCase.id(), relevance.isPass(), relevance.getScore(), relevance.getFeedback());

        assertThat(relevance.isPass())
                .as("Answer should be relevant")
                .isTrue();
        assertThat(relevance.getScore()).isGreaterThan(0.9f);
    }

    private Stream<Arguments> loadCases() throws IOException {
        var resource = new ClassPathResource("evaluation/orders/order-cases.json");

        List<FunctionalAgentEvaluationCase> cases =
                objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {
                });

        return cases.stream().map(c -> arguments(named(c.id(), c)));
    }
}
