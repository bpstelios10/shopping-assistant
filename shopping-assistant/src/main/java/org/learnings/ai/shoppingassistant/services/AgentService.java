package org.learnings.ai.shoppingassistant.services;

import jakarta.annotation.Nonnull;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;

public interface AgentService {

    ChatReplyDto chat(String message, @Nonnull String conversationId);
}
