package org.learnings.ai.shoppingassistant.services;

import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;

public interface ChatService {
    ChatReplyDto chat(String message);
}
