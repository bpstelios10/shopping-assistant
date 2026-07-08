package org.learnings.ai.shoppingassistant.services;

import org.learnings.ai.shoppingassistant.services.dtos.Message;
import org.springframework.stereotype.Service;

import java.util.NavigableSet;

@Service
public class MemoryServiceImpl implements MemoryService {

    private final MemoryServiceRepository repository;

    public MemoryServiceImpl(MemoryServiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public void addMessageToConversation(String conversationId, Message message) {
        repository.addMessageToConversation(conversationId, message);
    }

    @Override
    public NavigableSet<Message> getConversationHistory(String conversationId) {
        return repository.getConversation(conversationId);
    }
}
