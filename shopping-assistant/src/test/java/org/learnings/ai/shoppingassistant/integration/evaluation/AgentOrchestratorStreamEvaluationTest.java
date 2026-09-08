package org.learnings.ai.shoppingassistant.integration.evaluation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.learnings.ai.shoppingassistant.agents.AgentOrchestrator;
import org.learnings.ai.shoppingassistant.agents.AgentStreamEvent;
import org.learnings.ai.shoppingassistant.integration.evaluation.cases.EvaluationCase;
import org.learnings.ai.shoppingassistant.integration.evaluation.config.EvaluationConfig;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Flux;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest
@Import({EvaluationConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgentOrchestratorStreamEvaluationTest {

    @Autowired
    private AgentOrchestrator agentOrchestrator;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ChatMemory chatMemory;
    private String conversationId;

    @AfterEach
    void tearDown() {
        chatMemory.clear(conversationId);
    }

    @Test
    void supportAgentStream_whenAskedAboutRefundPolicy_shouldEmitOrderedEvents() throws IOException {
        EvaluationCase testCase = loadFirstCase();

        conversationId = "evaluation-" + testCase.id();
        Flux<AgentStreamEvent> stream = agentOrchestrator.chatStream(testCase.question(), conversationId);
        List<AgentStreamEvent> events = stream.collectList().block();

        assertThat(events).isNotNull().isNotEmpty();
        assertThat(events.getFirst()).isInstanceOf(AgentStreamEvent.Routed.class);
        assertThat(events.getLast()).isInstanceOf(AgentStreamEvent.Completed.class);

        String answer = events.stream()
                .filter(AgentStreamEvent.Delta.class::isInstance)
                .map(AgentStreamEvent.Delta.class::cast)
                .map(AgentStreamEvent.Delta::text)
                .collect(Collectors.joining());

        assertThat(answer).isNotBlank();
    }

    private EvaluationCase loadFirstCase() throws IOException {
        var resource = new ClassPathResource("evaluation/rag/rag-cases.json");

        List<EvaluationCase> cases = objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {
        });

        return cases.getFirst();
    }
}
