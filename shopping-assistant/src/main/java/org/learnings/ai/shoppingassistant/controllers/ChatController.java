package org.learnings.ai.shoppingassistant.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.learnings.ai.shoppingassistant.services.ChatService;
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
    public ResponseEntity<ChatReply> chat(@Valid @RequestBody CreateChat request) {

        String chatResponse = chatService.chat(request.message());

        return ResponseEntity.ok().body(new ChatReply(chatResponse));
    }

    public record CreateChat(@NotBlank String message) { }

    public record ChatReply(String message) { }
}
