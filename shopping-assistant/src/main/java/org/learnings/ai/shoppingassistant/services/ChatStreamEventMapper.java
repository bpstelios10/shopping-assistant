package org.learnings.ai.shoppingassistant.services;

import org.learnings.ai.shoppingassistant.agents.AgentStreamEvent;
import org.learnings.ai.shoppingassistant.services.dtos.ChatStreamEventDto;

public final class ChatStreamEventMapper {

    private ChatStreamEventMapper() {
    }

    public static ChatStreamEventDto toDto(AgentStreamEvent event) {
        return switch (event) {
            case AgentStreamEvent.Routed e ->
                    new ChatStreamEventDto.Routed(e.agent(), e.conversationId(), e.confidence());
            case AgentStreamEvent.Delta e ->
                    new ChatStreamEventDto.Delta(e.text());
            case AgentStreamEvent.ToolInvoked e ->
                    new ChatStreamEventDto.ToolCalled(
                            e.toolCall().id(), e.toolCall().type(), e.toolCall().name(), e.toolCall().arguments());
            case AgentStreamEvent.Completed e ->
                    new ChatStreamEventDto.Done(e.model(), e.promptTokens(), e.completionTokens());
            case AgentStreamEvent.Failed e ->
                    new ChatStreamEventDto.Error(e.message());
        };
    }
}
