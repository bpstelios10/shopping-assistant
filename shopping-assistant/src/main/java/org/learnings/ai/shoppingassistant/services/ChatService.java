package org.learnings.ai.shoppingassistant.services;

import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.learnings.ai.shoppingassistant.services.dtos.ChatStreamEventDto;
import reactor.core.publisher.Flux;

public interface ChatService {

    ChatReplyDto chat(String message, String conversationId);

    /**
     * Streaming variant. The returned flux always terminates normally: failures are delivered as a terminal
     * {@link ChatStreamEventDto.Error} event, because once the response is committed there is no status code left
     * to fail with.
     */
    Flux<ChatStreamEventDto> chatStream(String message, String conversationId);
}
