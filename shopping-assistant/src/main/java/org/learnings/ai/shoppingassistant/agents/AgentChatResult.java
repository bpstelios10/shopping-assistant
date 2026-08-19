package org.learnings.ai.shoppingassistant.agents;

import lombok.Builder;
import org.learnings.ai.shoppingassistant.advisors.ToolCallAuditingValues;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.List;

@Builder
public record AgentChatResult(
        ChatResponse chatResponse,
        List<AssistantMessage.ToolCall> toolCalls
) {
    public static AgentChatResult from(ChatClientResponse chatClientResponse) {
        return AgentChatResult.builder()
                .chatResponse(chatClientResponse.chatResponse())
                .toolCalls(ToolCallAuditingValues.getToolCalls(chatClientResponse.context()))
                .build();
    }
}
