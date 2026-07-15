package org.learnings.ai.shoppingassistant.agents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.services.memory.CurrentUser;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.learnings.ai.shoppingassistant.agents.RouterAgent.RoutingDecision.AgentType.SHOPPING;
import static org.learnings.ai.shoppingassistant.agents.RouterAgent.RoutingDecision.AgentType.SUPPORT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorTest {

    private static final String USER_ID = "anon:sess-abc";

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
        ChatResponse expected = response("hello");
        when(routerAgent.route("hi")).thenReturn(new RouterAgent.RoutingDecision(SHOPPING, 0.9));
        when(firstAgent.chat(eq("hi"), eq(USER_ID + ":conv-1"))).thenReturn(expected);

        ChatResponse actual = orchestrator.chat("hi", "conv-1");

        assertThat(actual).isSameAs(expected);
        verifyNoMoreInteractions(firstAgent, secondAgent, routerAgent);
    }

    @Test
    void chat_whenSupportQuestion_routesToSupportAgent() {
        ChatResponse expected = response("hello");
        when(routerAgent.route("hi")).thenReturn(new RouterAgent.RoutingDecision(SUPPORT, 0.9));
        when(secondAgent.chat(eq("hi"), eq(USER_ID + ":conv-1"))).thenReturn(expected);

        ChatResponse actual = orchestrator.chat("hi", "conv-1");

        assertThat(actual).isSameAs(expected);
        verifyNoMoreInteractions(firstAgent, secondAgent, routerAgent);
    }

    @Test
    void chat_whenNotSure_defaultsToShoppingAgent() {
        ChatResponse expected = response("hello");
        when(routerAgent.route("hi")).thenReturn(new RouterAgent.RoutingDecision(SUPPORT, 0.5));
        when(firstAgent.chat(eq("hi"), eq(USER_ID + ":conv-1"))).thenReturn(expected);

        ChatResponse actual = orchestrator.chat("hi", "conv-1");

        assertThat(actual).isSameAs(expected);
        verifyNoMoreInteractions(firstAgent, secondAgent, routerAgent);
    }

    @Test
    void chat_whenFallbackAgentWrong_throwsException() {
        AgentOrchestrator badOrchestrator = new AgentOrchestrator(List.of(secondAgent), routerAgent);
        when(routerAgent.route("hi")).thenReturn(new RouterAgent.RoutingDecision(SUPPORT, 0.5));

        assertThatThrownBy(() -> badOrchestrator.chat("hi", "conv-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No fallback 'shopping' agent configured");
        verifyNoMoreInteractions(firstAgent, secondAgent, routerAgent);
    }

    @Test
    void chat_whenBlankConversationId_generatesRandomId() {
        ArgumentCaptor<String> convId = ArgumentCaptor.forClass(String.class);
        when(routerAgent.route("hi")).thenReturn(new RouterAgent.RoutingDecision(SHOPPING, 0.9));
        when(firstAgent.chat(eq("hi"), convId.capture())).thenReturn(response("hello"));

        orchestrator.chat("hi", "  ");

        assertThat(convId.getValue())
                .startsWith(USER_ID + ":")
                .isNotEqualTo(USER_ID + ":  ");
        verifyNoMoreInteractions(firstAgent, secondAgent, routerAgent);
    }

    @Test
    void chat_setsCurrentUserDuringAgentCallAndClearsAfter() {
        AtomicReference<String> userDuringCall = new AtomicReference<>();
        when(routerAgent.route("hi")).thenReturn(new RouterAgent.RoutingDecision(SHOPPING, 0.9));
        when(firstAgent.chat(any(), any())).thenAnswer(_ -> {
            userDuringCall.set(CurrentUser.get());
            return response("hello");
        });

        orchestrator.chat("hi", "conv-1");

        assertThat(userDuringCall.get()).isEqualTo(USER_ID);
        assertThat(CurrentUser.get()).isNull();
        verifyNoMoreInteractions(firstAgent, secondAgent, routerAgent);
    }

    @SuppressWarnings("SameParameterValue")
    private static ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
