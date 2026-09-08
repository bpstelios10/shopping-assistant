package org.learnings.ai.shoppingassistant.services;

import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.agents.AgentStreamEvent;
import org.learnings.ai.shoppingassistant.services.dtos.ChatStreamEventDto;
import org.springframework.ai.chat.messages.AssistantMessage;

import static org.assertj.core.api.Assertions.assertThat;

class ChatStreamEventMapperTest {

    @Test
    void toDto_mapsRouted() {
        ChatStreamEventDto dto = ChatStreamEventMapper.toDto(
                new AgentStreamEvent.Routed("support", "conv-1", 0.87));

        assertThat(dto).isEqualTo(new ChatStreamEventDto.Routed("support", "conv-1", 0.87));
        assertThat(dto.event()).isEqualTo(ChatStreamEventDto.ROUTED);
    }

    @Test
    void toDto_mapsDelta() {
        ChatStreamEventDto dto = ChatStreamEventMapper.toDto(new AgentStreamEvent.Delta("hi"));

        assertThat(dto).isEqualTo(new ChatStreamEventDto.Delta("hi"));
        assertThat(dto.event()).isEqualTo(ChatStreamEventDto.DELTA);
    }

    @Test
    void toDto_mapsToolInvoked() {
        AssistantMessage.ToolCall toolCall =
                new AssistantMessage.ToolCall("id-1", "function", "searchProducts", "{\"q\":\"shoes\"}");

        ChatStreamEventDto dto = ChatStreamEventMapper.toDto(new AgentStreamEvent.ToolInvoked(toolCall));

        assertThat(dto).isEqualTo(
                new ChatStreamEventDto.ToolCalled("id-1", "function", "searchProducts", "{\"q\":\"shoes\"}"));
        assertThat(dto.event()).isEqualTo(ChatStreamEventDto.TOOL);
    }

    @Test
    void toDto_mapsCompleted() {
        ChatStreamEventDto dto = ChatStreamEventMapper.toDto(
                new AgentStreamEvent.Completed("qwen3:8b", 10, 20));

        assertThat(dto).isEqualTo(new ChatStreamEventDto.Done("qwen3:8b", 10, 20));
        assertThat(dto.event()).isEqualTo(ChatStreamEventDto.DONE);
    }

    @Test
    void toDto_mapsCompletedWithMissingUsage() {
        ChatStreamEventDto dto = ChatStreamEventMapper.toDto(
                new AgentStreamEvent.Completed(null, null, null));

        assertThat(dto).isEqualTo(new ChatStreamEventDto.Done(null, null, null));
    }

    @Test
    void toDto_mapsFailed() {
        ChatStreamEventDto dto = ChatStreamEventMapper.toDto(new AgentStreamEvent.Failed("boom"));

        assertThat(dto).isEqualTo(new ChatStreamEventDto.Error("boom"));
        assertThat(dto.event()).isEqualTo(ChatStreamEventDto.ERROR);
    }
}
