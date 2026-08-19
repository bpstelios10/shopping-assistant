package org.learnings.ai.shoppingassistant.agents;

public interface Agent {

    AgentChatResult chat(String message, String conversationId);

    String name();
}
