package org.learnings.ai.shoppingassistant.agents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.learnings.ai.shoppingassistant.agents.RouterAgent.RoutingStep.AgentType.SHOPPING;
import static org.learnings.ai.shoppingassistant.agents.RouterAgent.RoutingStep.AgentType.SUPPORT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorTest {

    private static final String USER_ID = "anon:sess-abc";
    private static final String CONV_ID = USER_ID + ":conv-1";

    @Mock
    private Agent firstAgent;
    @Mock
    private Agent secondAgent;
    @Mock
    private RouterAgent routerAgent;

    private AgentOrchestrator orchestrator;

    @BeforeEach
    void setup() {
        when(firstAgent.name()).thenReturn("shopping");
        when(secondAgent.name()).thenReturn("support");
        orchestrator = new AgentOrchestrator(List.of(firstAgent, secondAgent), routerAgent);
    }

    @Test
    void chat_whenShoppingQuestion_routesToShoppingAgent() {
        AgentChatResult expected = response("hello");
        routeTo(SHOPPING, "hello", 0.9);
        when(firstAgent.chat(new AgentRequest("hello", CONV_ID, USER_ID))).thenReturn(expected);

        AgentChatResult actual = orchestrator.chat("hi", "conv-1");

        assertThat(actual).isSameAs(expected);
        verifyNoMoreInteractions(firstAgent, secondAgent, routerAgent);
    }

    @Test
    void chat_whenSupportQuestion_routesToSupportAgent() {
        AgentChatResult expected = response("hello");
        routeTo(SUPPORT, "hello", 0.9);
        when(secondAgent.chat(new AgentRequest("hello", CONV_ID, USER_ID))).thenReturn(expected);

        AgentChatResult actual = orchestrator.chat("hi", "conv-1");

        assertThat(actual).isSameAs(expected);
        verifyNoMoreInteractions(firstAgent, secondAgent, routerAgent);
    }

    @Test
    void chat_whenNoDecision_defaultsToShoppingAgent() {
        AgentChatResult expected = response("hi");
        when(routerAgent.route("hi")).thenReturn(new RouterAgent.RoutingPlan(List.of()));
        when(firstAgent.chat(new AgentRequest("hi", CONV_ID, USER_ID))).thenReturn(expected);

        AgentChatResult actual = orchestrator.chat("hi", "conv-1");

        assertThat(actual).isSameAs(expected);
        verifyNoMoreInteractions(firstAgent, secondAgent, routerAgent);
    }

    @Test
    void chat_whenNotSure_defaultsToShoppingAgent() {
        AgentChatResult expected = response("hello");
        routeTo(SUPPORT, "hello", 0.5);
        when(firstAgent.chat(new AgentRequest("hello", CONV_ID, USER_ID))).thenReturn(expected);

        AgentChatResult actual = orchestrator.chat("hi", "conv-1");

        assertThat(actual).isSameAs(expected);
        verifyNoMoreInteractions(firstAgent, secondAgent, routerAgent);
    }

    @Test
    void chat_whenFallbackAgentWrong_throwsException() {
        AgentOrchestrator badOrchestrator = new AgentOrchestrator(List.of(secondAgent), routerAgent);
        routeTo(SUPPORT, "hello", 0.5);

        assertThatThrownBy(() -> badOrchestrator.chat("hi", "conv-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No fallback 'shopping' agent configured");
        verifyNoMoreInteractions(firstAgent, secondAgent, routerAgent);
    }

    @Test
    void chat_whenBlankConversationId_generatesRandomId() {
        ArgumentCaptor<AgentRequest> request = ArgumentCaptor.forClass(AgentRequest.class);
        routeTo(SHOPPING, "hi", 0.9);
        when(firstAgent.chat(request.capture())).thenReturn(response("hello"));

        orchestrator.chat("hi", "  ");

        assertThat(request.getValue().conversationId())
                .startsWith(USER_ID + ":")
                .isNotEqualTo(USER_ID + ":  ");
        verifyNoMoreInteractions(firstAgent, secondAgent, routerAgent);
    }

    @Test
    void chat_passesResolvedUserIdToAgent() {
        ArgumentCaptor<AgentRequest> request = ArgumentCaptor.forClass(AgentRequest.class);
        routeTo(SHOPPING, "hi", 0.9);
        when(firstAgent.chat(request.capture())).thenReturn(response("hello"));

        orchestrator.chat("hi", "conv-1");

        assertThat(request.getValue().userId()).isEqualTo(USER_ID);
    }

    @Test
    void chatStream_emitsRoutedEventBeforeAgentEvents() {
        routeTo(SUPPORT, "hello", 0.9);
        when(secondAgent.chatStream(new AgentRequest("hello", CONV_ID, USER_ID)))
                .thenReturn(Flux.just(new AgentStreamEvent.Delta("hi")));

        StepVerifier.create(orchestrator.chatStream("hi", "conv-1"))
                .expectNext(new AgentStreamEvent.Routed("support", CONV_ID, 0.9))
                .expectNext(new AgentStreamEvent.Delta("hi"))
                .verifyComplete();
    }

    @Test
    void chatStream_whenLowConfidence_routesToFallbackAgent() {
        routeTo(SUPPORT, "hello", 0.4);
        when(firstAgent.chatStream(new AgentRequest("hello", CONV_ID, USER_ID)))
                .thenReturn(Flux.just(new AgentStreamEvent.Delta("hi")));

        StepVerifier.create(orchestrator.chatStream("hi", "conv-1"))
                .expectNext(new AgentStreamEvent.Routed("shopping", CONV_ID, 0.4))
                .expectNext(new AgentStreamEvent.Delta("hi"))
                .verifyComplete();
    }

    @Test
    void chatStream_isColdSoRoutingDoesNotRunUntilSubscribed() {
        Flux<AgentStreamEvent> stream = orchestrator.chatStream("hi", "conv-1");

        verifyNoMoreInteractions(routerAgent, firstAgent, secondAgent);
        assertThat(stream).isNotNull();
    }

    @Test
    void chatStream_whenRoutingFails_propagatesError() {
        when(routerAgent.route("hi")).thenThrow(new IllegalStateException("router down"));

        StepVerifier.create(orchestrator.chatStream("hi", "conv-1"))
                .verifyErrorMessage("router down");
    }

    @Test
    void chatStream_whenAgentStreamFails_propagatesError() {
        routeTo(SHOPPING, "hi", 0.9);
        when(firstAgent.chatStream(any()))
                .thenReturn(Flux.error(new IllegalStateException("model exploded")));

        StepVerifier.create(orchestrator.chatStream("hi", "conv-1"))
                .expectNext(new AgentStreamEvent.Routed("shopping", CONV_ID, 0.9))
                .verifyErrorMessage("model exploded");
    }

    private void routeTo(RouterAgent.RoutingStep.AgentType agent, String task, double confidence) {
        when(routerAgent.route("hi")).thenReturn(
                new RouterAgent.RoutingPlan(List.of(new RouterAgent.RoutingStep(agent, task, confidence))));
    }

    @SuppressWarnings("SameParameterValue")
    private static AgentChatResult response(String text) {
        return AgentChatResult.builder().chatResponse(
                new ChatResponse(List.of(new Generation(new AssistantMessage(text))))
        ).build();
    }
}
