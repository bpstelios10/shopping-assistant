package org.learnings.ai.shoppingassistant.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.learnings.ai.shoppingassistant.tools.ProductTool;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentServiceImplTest {

    private static final String CONVERSATION_ID = "some-conversation-id";

    @Mock
    private ChatClient chatClient;
    @Mock
    private PromptService promptService;
    @Mock
    private ProductTool productTool;

    private AgentServiceImpl chatServiceImpl;

    @BeforeEach
    void setUp() {
        chatServiceImpl = new AgentServiceImpl(chatClient, promptService, List.of(productTool));
    }

    @Test
    void chat_whenCorrectInput_returnsResponse() {
        String message = "some message";
        Prompt prompt = new Prompt(message);
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(promptService.buildShoppingAssistantPrompt(eq(message))).thenReturn(prompt);
        when(chatClient.prompt(prompt)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.tools(productTool)).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(DefaultChatClient.DefaultCallResponseSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("some response"))));
        when(callResponseSpec.chatResponse()).thenReturn(chatResponse);

        ChatReplyDto response = chatServiceImpl.chat(message, CONVERSATION_ID);

        assertThat(response.generations()).hasSize(1);
        assertThat(response.generations().getFirst().text()).isEqualTo("some response");
        verifyNoMoreInteractions(chatClient, promptService, productTool, requestSpec, callResponseSpec);
    }

    @Test
    void chat_whenClientThrows_throwsException() {
        String message = "some message";
        Prompt prompt = new Prompt(message);
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(promptService.buildShoppingAssistantPrompt(eq(message))).thenReturn(prompt);
        when(chatClient.prompt(prompt)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.tools(productTool)).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("connection failed"));

        assertThatThrownBy(() -> chatServiceImpl.chat(message, CONVERSATION_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("connection failed");

        verifyNoMoreInteractions(chatClient, promptService, productTool, requestSpec);
    }

    @Test
    void chat_whenNoResponse_throwsException() {
        String message = "some message";
        Prompt prompt = new Prompt(message);
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(promptService.buildShoppingAssistantPrompt(eq(message))).thenReturn(prompt);
        when(chatClient.prompt(prompt)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.tools(productTool)).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(DefaultChatClient.DefaultCallResponseSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenReturn(null);

        assertThatThrownBy(() -> chatServiceImpl.chat(message, CONVERSATION_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Agent didnt reply");

        verifyNoMoreInteractions(chatClient, promptService, productTool, requestSpec, callResponseSpec);
    }
}
