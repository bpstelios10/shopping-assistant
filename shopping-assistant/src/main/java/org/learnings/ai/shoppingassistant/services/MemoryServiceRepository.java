package org.learnings.ai.shoppingassistant.services;

import org.learnings.ai.shoppingassistant.services.dtos.Message;

import java.util.NavigableSet;

public interface MemoryServiceRepository {

    void addMessageToConversation(String conversationId, Message message);

    NavigableSet<Message> getConversation(String conversationId);
}
