package org.learnings.ai.shoppingassistant.integration;

import org.hamcrest.Matchers;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.learnings.ai.shoppingassistant.agents.AgentChatResult;
import org.learnings.ai.shoppingassistant.agents.ShoppingAgent;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("integration")
@SpringBootTest(properties = {"clients.services.products.base-url=http://shopping-assistant"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShoppingAgentEvaluationTest {

    private static final Logger log = LoggerFactory.getLogger(ShoppingAgentEvaluationTest.class);

    @Autowired
    private ShoppingAgent shoppingAgent;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private MockRestServiceServer server;

    private String conversationId;

    @BeforeEach
    void setup() {
        server.reset();
        server.expect(ExpectedCount.max(1), requestTo("http://shopping-assistant/products/categories"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        ["MAGNET","POSTCARD","ACCESSORY","JEWELRY","CLOTHES"]
                        """, MediaType.APPLICATION_JSON));
    }

    @AfterEach
    void tearDown() {
        chatMemory.clear(conversationId);
    }

    @ParameterizedTest
    @MethodSource("loadCases")
    void shoppingAgent_whenAskedAboutRefundsOrShippingOrFaq_shouldPassTheEvaluation(FunctionalAgentEvaluationCase testCase) {
        ToolCallingEvaluator toolCallingEvaluator = new ToolCallingEvaluator(objectMapper, testCase.expectedTools());
//        // because we use mock products service, we need to have both cases here unfortunately
        server.expect(ExpectedCount.manyTimes(),
                        requestTo(Matchers.startsWith("http://shopping-assistant/products/search")))
                .andExpect(method(GET))
                .andRespond(productsSearchResponse());

        conversationId = "evaluation-" + testCase.id();
        AgentChatResult agentChatResult = shoppingAgent.chat(testCase.question(), conversationId);

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
        assertThat(relevance.getScore()).isGreaterThan(0.3f);
    }

    private Stream<Arguments> loadCases() throws IOException {
        var resource = new ClassPathResource("evaluation/shopping/shopping-cases.json");

        List<FunctionalAgentEvaluationCase> cases =
                objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {
                });

        return cases.stream().map(c -> arguments(named(c.id(), c)));
    }

    private ResponseCreator productsSearchResponse() {
        return request -> {
            if (request == null) {
                throw new IllegalArgumentException("request is null");
            }
            Map<String, List<String>> params = UriComponentsBuilder.fromUri(request.getURI())
                    .build()
                    .getQueryParams();

            List<String> query = params.get("query");
            List<String> category = params.get("category");

            String body = getString(request, query, category);

            MockClientHttpResponse response =
                    new MockClientHttpResponse(body.getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return response;
        };
    }

    private static @NonNull String getString(ClientHttpRequest request, List<String> query, List<String> category) {
        String queryValue = query != null && query.size() == 1 ? query.getFirst() : null;
        String categoryValue = category != null && category.size() == 1 ? category.getFirst() : null;

        String body;
        if ("socks".equals(queryValue) && categoryValue == null) {
            body = "[]";
        } else if ("socks".equals(queryValue) && "CLOTHES".equals(categoryValue)) {
            body = "[]";
        } else if ("clothing".equals(queryValue) && "CLOTHES".equals(categoryValue)) {
            body = "[]";
        } else if (queryValue == null && "CLOTHES".equals(categoryValue)) {
            body = """
                    [{"id":"11111111-1111-1111-1111-111111111111","name":"Hoodie X","category":"clothes","price":45.0}]
                    """;
        } else {
            throw new AssertionError("Unexpected /products/search request: " + request.getURI());
        }
        return body;
    }
}
