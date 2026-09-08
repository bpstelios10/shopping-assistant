package org.learnings.ai.shoppingassistant.services;

import org.learnings.ai.shoppingassistant.agents.AgentStreamEvent;
import org.learnings.ai.shoppingassistant.agents.AgentStreamEvent.Failed;
import org.learnings.ai.shoppingassistant.services.dtos.ChatStreamEventDto;

import static org.learnings.ai.shoppingassistant.agents.AgentStreamEvent.*;

public final class ChatStreamEventMapper {

    private ChatStreamEventMapper() {
    }

    public static ChatStreamEventDto toDto(AgentStreamEvent event) {
        return switch (event) {
            case Routed e -> new ChatStreamEventDto.Routed(e.agent(), e.conversationId(), e.confidence());
            case Delta e -> new ChatStreamEventDto.Delta(e.text());
            case ToolInvoked e -> new ChatStreamEventDto.ToolCalled(
                    e.toolCall().id(), e.toolCall().type(), e.toolCall().name(), e.toolCall().arguments());
            case Completed e -> new ChatStreamEventDto.Done(e.model(), e.promptTokens(), e.completionTokens());
            case Failed e -> new ChatStreamEventDto.Error(e.message());
        };
    }
}
