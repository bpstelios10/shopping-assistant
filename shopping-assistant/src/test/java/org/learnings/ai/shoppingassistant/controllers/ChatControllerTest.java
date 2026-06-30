package org.learnings.ai.shoppingassistant.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.services.ChatService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

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
        when(chatService.chat(message)).thenReturn("some response");
        ChatController.CreateChat request = new ChatController.CreateChat(message);

        ResponseEntity<ChatController.ChatReply> response = chatController.chat(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(new ChatController.ChatReply("some response"));
        verifyNoMoreInteractions(chatService);
    }

    @Test
    void chat_whenServiceThrows_throwsException() {
        String message = "some message";
        when(chatService.chat(message)).thenThrow(new RuntimeException("some error"));
        ChatController.CreateChat request = new ChatController.CreateChat(message);

        assertThatThrownBy(() -> chatController.chat(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("some error");
    }
}
