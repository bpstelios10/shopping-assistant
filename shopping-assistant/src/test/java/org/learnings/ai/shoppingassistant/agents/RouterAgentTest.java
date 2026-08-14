package org.learnings.ai.shoppingassistant.agents;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

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

    @ParameterizedTest
    @MethodSource("provideAllRoutingDecisions")
    void route_whenSingleDecisionPlan_returnsPlanWith1Decision(
            String message, List<RouterAgent.RoutingStep> decisions) {
        RouterAgent.RoutingStep expected = getRoutingDecision(message, decisions).decisions().getFirst();

        RouterAgent.RoutingPlan plan = routerAgent.route(message);
        RouterAgent.RoutingStep decision = plan.decisions().getFirst();

        assertThat(decision).isSameAs(expected);
        verifyNoMoreInteractions(chatClient);
    }

    @Test
    void route_whenMultipleDecisionPlan_returnsList() {
        String message = "where is my order X? and what is your delivery policy?";
        RouterAgent.RoutingPlan expectedPlan =
                getRoutingDecision(message, List.of(
                        new RouterAgent.RoutingStep(
                                RouterAgent.RoutingStep.AgentType.ORDERS, "where is my order X?", 0.88),
                        new RouterAgent.RoutingStep(
                                RouterAgent.RoutingStep.AgentType.SUPPORT, "what is your delivery policy?", 0.89)));

        RouterAgent.RoutingPlan plan = routerAgent.route(message);

        assertThat(plan.decisions()).hasSize(2);
        RouterAgent.RoutingStep decision = plan.decisions().getFirst();
        assertThat(decision).isSameAs(expectedPlan.decisions().getFirst());
        decision = plan.decisions().get(1);
        assertThat(decision).isSameAs(expectedPlan.decisions().get(1));
        verifyNoMoreInteractions(chatClient);
    }

    @SuppressWarnings("unchecked")
    @Test
    void route_whenClientThrows_propagatesException() {
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("connection failed"));

        assertThatThrownBy(() -> routerAgent.route("hi"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("connection failed");
        verifyNoMoreInteractions(chatClient);
    }

    private static Stream<Arguments> provideAllRoutingDecisions() {
        return Stream.of(
                Arguments.of("do you have red shoes?", List.of(new RouterAgent.RoutingStep(
                        RouterAgent.RoutingStep.AgentType.SHOPPING, "do you have red shoes?", 0.92))),
                Arguments.of("what is your refund policy?", List.of(new RouterAgent.RoutingStep(
                        RouterAgent.RoutingStep.AgentType.SUPPORT, "what is your refund policy?", 0.88))),
                Arguments.of("where is my order X?", List.of(new RouterAgent.RoutingStep(
                        RouterAgent.RoutingStep.AgentType.ORDERS, "where is my order X?", 0.9)))
        );
    }

    @SuppressWarnings("unchecked")
    private RouterAgent.RoutingPlan getRoutingDecision(String text, List<RouterAgent.RoutingStep> decisions) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        ChatClient.AdvisorSpec advisorSpec = mock(ChatClient.AdvisorSpec.class);
        when(advisorSpec.param("agent", "router")).thenReturn(advisorSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenAnswer(inv -> {
            Consumer<ChatClient.AdvisorSpec> c = inv.getArgument(0);
            c.accept(advisorSpec);
            return requestSpec;
        });
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(text)).thenReturn(requestSpec);
        ChatClient.CallResponseSpec mockResponse = mock(ChatClient.CallResponseSpec.class);
        when(requestSpec.call()).thenReturn(mockResponse);
        RouterAgent.RoutingPlan expectedPlan = new RouterAgent.RoutingPlan(decisions);
        when(mockResponse.entity(eq(RouterAgent.RoutingPlan.class))).thenReturn(expectedPlan);
        return expectedPlan;
    }
}
