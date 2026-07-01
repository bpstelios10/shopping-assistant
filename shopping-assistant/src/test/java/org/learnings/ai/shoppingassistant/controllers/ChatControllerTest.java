package org.learnings.ai.shoppingassistant.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.services.AgentService;
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
    private AgentService agentService;
    @InjectMocks
    private ChatController chatController;

    @Test
    void chat_whenCorrectInput_succeeds() {
        String message = "some message";
        ChatReplyDto reply = new ChatReplyDto("qwen3:8b", 10, 20, List.of());
        when(agentService.chat(message)).thenReturn(reply);
        ChatController.CreateChat request = new ChatController.CreateChat(message);

        ResponseEntity<ChatReplyDto> response = chatController.chat(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(reply);
        verifyNoMoreInteractions(agentService);
    }

    @Test
    void chat_whenServiceThrows_throwsException() {
        String message = "some message";
        when(agentService.chat(message)).thenThrow(new RuntimeException("some error"));
        ChatController.CreateChat request = new ChatController.CreateChat(message);

        assertThatThrownBy(() -> chatController.chat(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("some error");
    }
}
