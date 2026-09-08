package org.learnings.ai.shoppingassistant.web.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.services.ChatService;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.learnings.ai.shoppingassistant.services.dtos.ChatStreamEventDto;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

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
        ChatReplyDto reply = new ChatReplyDto("qwen3:8b", conversationId, 10, 20, List.of(), List.of());
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

    @Test
    void chatStream_wrapsEachDtoInASeverSentEventNamedAfterTheEventType() {
        String message = "some message";
        String conversationId = "some-conversation-id";
        ChatStreamEventDto.Routed routed = new ChatStreamEventDto.Routed("support", conversationId, 0.9);
        ChatStreamEventDto.Delta delta = new ChatStreamEventDto.Delta("hi");
        ChatStreamEventDto.Done done = new ChatStreamEventDto.Done("qwen3:8b", 10, 20);
        when(chatService.chatStream(message, conversationId)).thenReturn(Flux.just(routed, delta, done));

        Flux<ServerSentEvent<ChatStreamEventDto>> events =
                chatController.chatStream(new ChatController.CreateChat(message, conversationId));

        StepVerifier.create(events)
                .assertNext(event -> {
                    assertThat(event.event()).isEqualTo(ChatStreamEventDto.ROUTED);
                    assertThat(event.data()).isEqualTo(routed);
                })
                .assertNext(event -> {
                    assertThat(event.event()).isEqualTo(ChatStreamEventDto.DELTA);
                    assertThat(event.data()).isEqualTo(delta);
                })
                .assertNext(event -> {
                    assertThat(event.event()).isEqualTo(ChatStreamEventDto.DONE);
                    assertThat(event.data()).isEqualTo(done);
                })
                .verifyComplete();
        verifyNoMoreInteractions(chatService);
    }

    @Test
    void chatStream_whenServiceEmitsErrorEvent_stillCompletesNormally() {
        String message = "some message";
        when(chatService.chatStream(message, null))
                .thenReturn(Flux.just(new ChatStreamEventDto.Error("boom")));

        StepVerifier.create(chatController.chatStream(new ChatController.CreateChat(message, null)))
                .assertNext(event -> assertThat(event.event()).isEqualTo(ChatStreamEventDto.ERROR))
                .verifyComplete();
    }
}
