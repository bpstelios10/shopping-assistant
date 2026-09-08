package org.learnings.ai.shoppingassistant.web.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.learnings.ai.shoppingassistant.services.ChatService;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.learnings.ai.shoppingassistant.services.dtos.ChatStreamEventDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatReplyDto> chat(@Valid @RequestBody CreateChat request) {
        ChatReplyDto chatResponse = chatService.chat(request.message(), request.conversationId);

        return ResponseEntity.ok().body(chatResponse);
    }

    /**
     * Server-sent events variant of {@link #chat(CreateChat)}.
     *
     * <p>Returning a {@code Flux} from Spring MVC is adapted to an async servlet response, so the container thread is
     * released as soon as the response is committed and is not held for the duration of the inference.
     *
     * <p>Validation errors still surface as a normal 4xx, because {@code @Valid} runs before the flux is subscribed.
     * Anything that fails later is delivered as an {@code error} event by the service — see {@link ChatService#chatStream}.
     */
    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamEventDto>> chatStream(@Valid @RequestBody CreateChat request) {
        return chatService.chatStream(request.message(), request.conversationId())
                .map(event -> ServerSentEvent.builder(event)
                        .event(event.event())
                        .build());
    }

    public record CreateChat(@NotBlank String message, String conversationId) {
    }
}
