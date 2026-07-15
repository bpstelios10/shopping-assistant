package org.learnings.ai.shoppingassistant.agents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.agents.prompts.PromptProvider;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportAgentTest {

    private static final String CONVERSATION_ID = "some-conversation-id";

    @Mock
    private ChatClient.Builder chatClientBuilder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private QuestionAnswerAdvisor ragAdvisor;
    @Mock
    private PromptProvider promptProvider;

    private SupportAgent supportAgent;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.defaultAdvisors(ragAdvisor)).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        supportAgent = new SupportAgent(chatClientBuilder, ragAdvisor, promptProvider);
    }

    @Test
    void name_returnsSupport() {
        assertThat(supportAgent.name()).isEqualTo("support");
    }

    @SuppressWarnings("unchecked")
    @Test
    void chat_whenCorrectInput_returnsResponse() {
        String message = "some message";
        Prompt prompt = new Prompt(message);
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(promptProvider.buildPrompt(eq(message))).thenReturn(prompt);
        when(chatClient.prompt(prompt)).thenReturn(requestSpec);
        // use doAnswer one time to verify that conversation-id is correct
        doAnswer(invocation -> {
            Consumer<ChatClient.AdvisorSpec> consumer = invocation.getArgument(0);
            ChatClient.AdvisorSpec advisorSpec = mock(ChatClient.AdvisorSpec.class);
            when(advisorSpec.param(any(), any())).thenReturn(advisorSpec);
            consumer.accept(advisorSpec);
            verify(advisorSpec).param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID);

            return requestSpec;
        }).when(requestSpec).advisors(any(Consumer.class));
        ChatClient.CallResponseSpec callResponseSpec = mock(DefaultChatClient.DefaultCallResponseSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("some response"))));
        when(callResponseSpec.chatResponse()).thenReturn(chatResponse);

        ChatResponse response = supportAgent.chat(message, CONVERSATION_ID);

        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResult().getOutput().getText()).isEqualTo("some response");
        verifyNoMoreInteractions(chatClient, promptProvider, requestSpec, callResponseSpec);
    }

    @SuppressWarnings("unchecked")
    @Test
    void chat_whenClientThrows_throwsException() {
        String message = "some message";
        Prompt prompt = new Prompt(message);
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(promptProvider.buildPrompt(eq(message))).thenReturn(prompt);
        when(chatClient.prompt(prompt)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("connection failed"));

        assertThatThrownBy(() -> supportAgent.chat(message, CONVERSATION_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("connection failed");

        verifyNoMoreInteractions(chatClient, promptProvider, requestSpec);
    }

    @SuppressWarnings("unchecked")
    @Test
    void chat_whenNoResponse_throwsException() {
        String message = "some message";
        Prompt prompt = new Prompt(message);
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(promptProvider.buildPrompt(eq(message))).thenReturn(prompt);
        when(chatClient.prompt(prompt)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(DefaultChatClient.DefaultCallResponseSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenReturn(null);

        assertThatThrownBy(() -> supportAgent.chat(message, CONVERSATION_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Agent didnt reply");

        verifyNoMoreInteractions(chatClient, promptProvider, requestSpec, callResponseSpec);
    }
}