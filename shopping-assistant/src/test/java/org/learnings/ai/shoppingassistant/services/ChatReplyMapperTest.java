package org.learnings.ai.shoppingassistant.services;

import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChatReplyMapperTest {

    @Test
    void toChatReplyDto_mapsMetadataToolCallsAndReasoning() {
        AssistantMessage.ToolCall toolCall =
                new AssistantMessage.ToolCall("call-1", "function", "getProduct", "{\"id\":1}");
        AssistantMessage output = AssistantMessage.builder()
                .content("some response")
                .properties(Map.of("reasoningContent", "thinking..."))
                .toolCalls(List.of(toolCall))
                .build();
        ChatResponse response = new ChatResponse(
                List.of(new Generation(output)),
                ChatResponseMetadata.builder()
                        .model("qwen3:8b")
                        .usage(new DefaultUsage(10, 20))
                        .build());

        ChatReplyDto dto = ChatReplyMapper.toChatReplyDto(response, "conv-id");

        assertThat(dto.model()).isEqualTo("qwen3:8b");
        assertThat(dto.conversationId()).isEqualTo("conv-id");
        assertThat(dto.promptTokens()).isEqualTo(10);
        assertThat(dto.completionTokens()).isEqualTo(20);
        assertThat(dto.generations()).hasSize(1);

        ChatReplyDto.GenerationDto generation = dto.generations().getFirst();
        assertThat(generation.text()).isEqualTo("some response");
        assertThat(generation.messageType()).isEqualTo(MessageType.ASSISTANT.getValue());
        assertThat(generation.reasoningContent()).isEqualTo("thinking...");
        assertThat(generation.toolCalls()).containsExactly(
                new ChatReplyDto.ToolCall("call-1", "function", "getProduct", "{\"id\":1}"));
    }

    @Test
    void toChatReplyDto_whenNoToolCallsOrReasoning_mapsEmptyAndNull() {
        ChatResponse response = new ChatResponse(
                List.of(new Generation(new AssistantMessage("plain answer"))),
                ChatResponseMetadata.builder()
                        .model("qwen3:8b")
                        .usage(new DefaultUsage(1, 2))
                        .build());

        ChatReplyDto dto = ChatReplyMapper.toChatReplyDto(response, null);

        assertThat(dto.conversationId()).isEqualTo(null);
        ChatReplyDto.GenerationDto generation = dto.generations().getFirst();
        assertThat(generation.text()).isEqualTo("plain answer");
        assertThat(generation.toolCalls()).isEmpty();
        assertThat(generation.reasoningContent()).isNull();
    }
}
