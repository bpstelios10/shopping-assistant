package org.learnings.ai.shoppingassistant.infrastructure.repositories;

import jakarta.annotation.Nonnull;
import org.learnings.ai.shoppingassistant.services.MemoryServiceRepository;
import org.learnings.ai.shoppingassistant.services.dtos.Message;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Repository
public class MemoryServiceRepositoryInMemory implements MemoryServiceRepository {

    private final Map<String, ConcurrentSkipListSet<Message>> storage = new ConcurrentHashMap<>();

    @Override
    public void addMessageToConversation(String conversationId, Message message) {
        storage.computeIfAbsent(conversationId, _ -> new ConcurrentSkipListSet<>()).add(message);
    }

    @Override
    public @Nonnull NavigableSet<Message> getConversation(String conversationId) {
        return storage.getOrDefault(conversationId, new ConcurrentSkipListSet<>());
    }
}
