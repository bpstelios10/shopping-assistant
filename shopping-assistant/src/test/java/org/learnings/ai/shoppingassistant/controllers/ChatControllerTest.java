package org.learnings.ai.shoppingassistant.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.services.ChatService;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;
    @InjectMocks
    private ChatController chatController;

    @Test
    void chat_whenCorrectInput_succeeds() {
        String message = "some message";
        String conversationId = "some-conversation-id";
        ChatReplyDto reply = new ChatReplyDto("qwen3:8b", conversationId, 10, 20, List.of());
        when(chatService.chat(message, conversationId)).thenReturn(reply);
        ChatController.CreateChat request = new ChatController.CreateChat(message, conversationId);

        ResponseEntity<ChatReplyDto> response = chatController.chat(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(reply);
        verifyNoMoreInteractions(chatService);
    }

    @Test
    void chat_whenServiceThrows_throwsException() {
        String message = "some message";
        String conversationId = "some-conversation-id";
        when(chatService.chat(message, conversationId)).thenThrow(new RuntimeException("some error"));
        ChatController.CreateChat request = new ChatController.CreateChat(message, conversationId);

        assertThatThrownBy(() -> chatController.chat(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("some error");
    }
}
