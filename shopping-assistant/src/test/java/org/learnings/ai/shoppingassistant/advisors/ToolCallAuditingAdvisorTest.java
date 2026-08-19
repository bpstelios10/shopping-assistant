package org.learnings.ai.shoppingassistant.advisors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.learnings.ai.shoppingassistant.advisors.ToolCallAuditingValues.TOOL_CALLS_CONTEXT_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolCallAuditingAdvisorTest {

    @Mock
    private CallAdvisorChain chain;
    @Mock
    private ChatClientRequest request;
    @Mock
    private ChatClientResponse response;
    private final ToolCallAuditingAdvisor advisor = new ToolCallAuditingAdvisor();

    @Test
    void getOrder_returnsExpectedOrder() {
        assertThat(advisor.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 400);
    }

    @Test
    void getName_returnsExpectedName() {
        assertThat(advisor.getName()).isEqualTo("Tool Calls Auditor Advisor");
    }

    @Test
    void adviseCall_whenChatResponseIsNull_returnsResponseWithoutChanges() {
        when(chain.nextCall(request)).thenReturn(response);
        when(response.chatResponse()).thenReturn(null);

        ChatClientResponse result = advisor.adviseCall(request, chain);

        assertThat(result).isSameAs(response);
        verifyNoMoreInteractions(chain, response);
    }

    @Test
    void adviseCall_whenResultIsNull_returnsResponseWithoutChanges() {
        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chain.nextCall(request)).thenReturn(response);
        when(response.chatResponse()).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(null);

        ChatClientResponse result = advisor.adviseCall(request, chain);

        assertThat(result).isSameAs(response);
        verifyNoMoreInteractions(chain, response, chatResponse);
    }

    @Test
    void adviseCall_whenHasToolCallsIsFalse_returnsResponseWithoutContextMutation() {
        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chain.nextCall(request)).thenReturn(response);
        when(response.chatResponse()).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(mock(Generation.class));
        when(chatResponse.hasToolCalls()).thenReturn(false);

        ChatClientResponse result = advisor.adviseCall(request, chain);

        assertThat(result).isSameAs(response);
        verifyNoMoreInteractions(chain, response, chatResponse);
    }

    @Test
    void adviseCall_whenToolCallsExistAndContextContainsTypedList_addsAllToolCalls() {
        AssistantMessage.ToolCall toolCall1 = mock(AssistantMessage.ToolCall.class);
        AssistantMessage.ToolCall toolCall2 = mock(AssistantMessage.ToolCall.class);

        Generation generation = mock(Generation.class);
        AssistantMessage assistantMessage = mock(AssistantMessage.class);
        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);
        when(chatResponse.hasToolCalls()).thenReturn(true);

        List<AssistantMessage.ToolCall> contextToolCalls = new ArrayList<>();
        contextToolCalls.add(mock(AssistantMessage.ToolCall.class));

        Map<String, Object> context = new HashMap<>();
        context.put(TOOL_CALLS_CONTEXT_KEY, contextToolCalls);

        when(chain.nextCall(request)).thenReturn(response);
        when(response.chatResponse()).thenReturn(chatResponse);
        when(generation.getOutput()).thenReturn(assistantMessage);
        when(assistantMessage.getToolCalls()).thenReturn(List.of(toolCall1, toolCall2));
        when(response.context()).thenReturn(context);

        ChatClientResponse result = advisor.adviseCall(request, chain);

        assertThat(result).isSameAs(response);
        assertThat(contextToolCalls).hasSize(3);
        assertThat(contextToolCalls).contains(toolCall1, toolCall2);
        verifyNoMoreInteractions(chain, response, generation, assistantMessage);
    }
}
