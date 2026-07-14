package org.learnings.ai.shoppingassistant.agents;

import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.services.memory.CurrentUser;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentOrchestratorTest {

    private static final String USER_ID = "anon:sess-abc";

    private final Agent firstAgent = mock(Agent.class);
    private final Agent secondAgent = mock(Agent.class);
    private final AgentOrchestrator orchestrator =
            new AgentOrchestrator(List.of(firstAgent, secondAgent));

    private static ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Test
    void chat_routesToFirstAgent_withCompositeConversationId() {
        ChatResponse expected = response("hello");
        when(firstAgent.chat(eq("hi"), eq(USER_ID + ":conv-1"))).thenReturn(expected);

        ChatResponse actual = orchestrator.chat("hi", "conv-1");

        assertThat(actual).isSameAs(expected);
        verify(firstAgent).chat("hi", USER_ID + ":conv-1");
        verifyNoInteractions(secondAgent);
    }

    @Test
    void chat_whenBlankConversationId_generatesRandomId() {
        ArgumentCaptor<String> convId = ArgumentCaptor.forClass(String.class);
        when(firstAgent.chat(eq("hi"), convId.capture())).thenReturn(response("hello"));

        orchestrator.chat("hi", "  ");

        assertThat(convId.getValue())
                .startsWith(USER_ID + ":")
                .isNotEqualTo(USER_ID + ":  ");
    }

    @Test
    void chat_setsCurrentUserDuringAgentCall_andClearsAfter() {
        AtomicReference<String> userDuringCall = new AtomicReference<>();
        when(firstAgent.chat(any(), any())).thenAnswer(invocation -> {
            userDuringCall.set(CurrentUser.get());
            return response("hello");
        });

        orchestrator.chat("hi", "conv-1");

        assertThat(userDuringCall.get()).isEqualTo(USER_ID);
        assertThat(CurrentUser.get()).isNull();
    }
}