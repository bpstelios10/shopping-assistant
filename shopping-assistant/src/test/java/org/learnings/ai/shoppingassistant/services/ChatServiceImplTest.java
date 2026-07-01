package org.learnings.ai.shoppingassistant.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatClient chatClient;
    @Mock
    private PromptService promptService;
    @InjectMocks
    private ChatServiceImpl chatServiceImpl;

    @Test
    void chat_whenCorrectInput_returnsResponse() {
        String message = "some message";
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(promptService.shoppingAssistantPrompt()).thenReturn("some prompts");
        when(chatClient.prompt("some prompts")).thenReturn(requestSpec);
        when(requestSpec.user(message)).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(DefaultChatClient.DefaultCallResponseSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("some response"))));
        when(callResponseSpec.chatResponse()).thenReturn(chatResponse);

        ChatReplyDto response = chatServiceImpl.chat(message);

        assertThat(response.generations()).hasSize(1);
        assertThat(response.generations().getFirst().text()).isEqualTo("some response");
        verifyNoMoreInteractions(chatClient, requestSpec, callResponseSpec);
    }

    @Test
    void chat_whenClientThrows_throwsException() {
        String message = "some message";
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(promptService.shoppingAssistantPrompt()).thenReturn("some prompts");
        when(chatClient.prompt("some prompts")).thenReturn(requestSpec);
        when(requestSpec.user(message)).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("connection failed"));

        assertThatThrownBy(() -> chatServiceImpl.chat(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("connection failed");

        verifyNoMoreInteractions(chatClient, requestSpec);
    }

    @Test
    void chat_whenNoResponse_throwsException() {
        String message = "some message";
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(promptService.shoppingAssistantPrompt()).thenReturn("some prompts");
        when(chatClient.prompt("some prompts")).thenReturn(requestSpec);
        when(requestSpec.user(message)).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(DefaultChatClient.DefaultCallResponseSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenReturn(null);

        assertThatThrownBy(() -> chatServiceImpl.chat(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Agent didnt reply");

        verifyNoMoreInteractions(chatClient, requestSpec, callResponseSpec);
    }
}
