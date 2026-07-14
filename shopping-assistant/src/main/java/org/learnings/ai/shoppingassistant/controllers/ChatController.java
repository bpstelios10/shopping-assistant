package org.learnings.ai.shoppingassistant.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.learnings.ai.shoppingassistant.services.ChatService;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public record CreateChat(@NotBlank String message, String conversationId) {
    }
}
