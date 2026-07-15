package org.learnings.ai.shoppingassistant.agents;

import org.springframework.ai.chat.model.ChatResponse;

public interface Agent {

    ChatResponse chat(String message, String conversationId);

    String name();
}
