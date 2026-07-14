package org.learnings.ai.shoppingassistant.services;

import lombok.extern.slf4j.Slf4j;
import org.learnings.ai.shoppingassistant.agents.AgentOrchestrator;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final AgentOrchestrator agentOrchestrator;

    public ChatServiceImpl(AgentOrchestrator agentOrchestrator) {
        this.agentOrchestrator = agentOrchestrator;
    }

    @Override
    public ChatReplyDto chat(String message, String conversationId) {
        ChatResponse chatResponse = agentOrchestrator.chat(message, conversationId);

        return ChatReplyMapper.toChatReplyDto(chatResponse, conversationId);
    }
}
