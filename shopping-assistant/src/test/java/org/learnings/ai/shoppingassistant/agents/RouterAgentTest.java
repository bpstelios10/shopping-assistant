package org.learnings.ai.shoppingassistant.agents;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouterAgentTest {

    @Mock
    private ChatClient chatClient;
    @InjectMocks
    private RouterAgent routerAgent;

    @Test
    void route_whenShoppingMessage_returnsShoppingDecision() {
        RouterAgent.RoutingDecision expected = getRoutingDecision("do you have red shoes?", RouterAgent.RoutingDecision.AgentType.SHOPPING, 0.92);

        RouterAgent.RoutingDecision decision = routerAgent.route("do you have red shoes?");

        assertThat(decision).isSameAs(expected);
        verifyNoMoreInteractions(chatClient);
    }

    @Test
    void route_whenSupportMessage_returnsSupportDecision() {
        RouterAgent.RoutingDecision expected = getRoutingDecision("what is your refund policy?", RouterAgent.RoutingDecision.AgentType.SUPPORT, 0.88);

        RouterAgent.RoutingDecision decision = routerAgent.route("what is your refund policy?");

        assertThat(decision).isSameAs(expected);
        verifyNoMoreInteractions(chatClient);
    }

    @Test
    void route_whenClientThrows_propagatesException() {
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("connection failed"));

        assertThatThrownBy(() -> routerAgent.route("hi"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("connection failed");
        verifyNoMoreInteractions(chatClient);
    }

    private RouterAgent.RoutingDecision getRoutingDecision(String text, RouterAgent.RoutingDecision.AgentType shopping, double confidence) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(text)).thenReturn(requestSpec);
        ChatClient.CallResponseSpec mockResponse = mock(ChatClient.CallResponseSpec.class);
        when(requestSpec.call()).thenReturn(mockResponse);
        RouterAgent.RoutingDecision expected = new RouterAgent.RoutingDecision(shopping, confidence);
        when(mockResponse.entity(eq(RouterAgent.RoutingDecision.class))).thenReturn(expected);
        return expected;
    }
}
