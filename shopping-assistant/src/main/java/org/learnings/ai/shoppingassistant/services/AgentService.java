package org.learnings.ai.shoppingassistant.services;

import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;

public interface AgentService {

    ChatReplyDto chat(String message, String conversationId);
}
