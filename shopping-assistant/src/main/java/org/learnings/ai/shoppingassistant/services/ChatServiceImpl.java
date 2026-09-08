package org.learnings.ai.shoppingassistant.services;

import lombok.extern.slf4j.Slf4j;
import org.learnings.ai.shoppingassistant.agents.AgentChatResult;
import org.learnings.ai.shoppingassistant.agents.AgentOrchestrator;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.learnings.ai.shoppingassistant.services.dtos.ChatStreamEventDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    static final String FAILURE_MESSAGE =
            "Sorry, something went wrong while answering. Please try again.";
    static final String TIMEOUT_MESSAGE =
            "Sorry, that took too long to answer. Please try again.";

    private final AgentOrchestrator agentOrchestrator;
    private final Duration streamTimeout;

    public ChatServiceImpl(AgentOrchestrator agentOrchestrator,
                           @Value("${shopping-assistant.chat.stream-timeout:60s}") Duration streamTimeout) {
        this.agentOrchestrator = agentOrchestrator;
        this.streamTimeout = streamTimeout;
    }

    @Override
    public ChatReplyDto chat(String message, String conversationId) {
        AgentChatResult chatResponse = agentOrchestrator.chat(message, conversationId);

        return ChatReplyMapper.toChatReplyDto(chatResponse, conversationId);
    }

    @Override
    public Flux<ChatStreamEventDto> chatStream(String message, String conversationId) {
        return agentOrchestrator.chatStream(message, conversationId)
                .map(ChatStreamEventMapper::toDto)
                .timeout(streamTimeout, Flux.just(new ChatStreamEventDto.Error(TIMEOUT_MESSAGE)))
                .onErrorResume(error -> {
                    log.error("chat stream failed for conversation [{}]", conversationId, error);
                    return Flux.just(new ChatStreamEventDto.Error(FAILURE_MESSAGE));
                })
                .doOnCancel(() -> log.debug("client cancelled chat stream for conversation [{}]", conversationId));
    }
}
