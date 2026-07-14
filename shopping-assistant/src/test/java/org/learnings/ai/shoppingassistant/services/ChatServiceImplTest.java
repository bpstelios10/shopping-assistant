package org.learnings.ai.shoppingassistant.services;

import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.agents.AgentOrchestrator;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplTest {

    private final AgentOrchestrator orchestrator = mock(AgentOrchestrator.class);
    private final ChatServiceImpl chatService = new ChatServiceImpl(orchestrator);

    @Test
    void chat_delegatesToOrchestrator_andMapsResponse() {
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model("some-model")
                .usage(new DefaultUsage(10, 20))
                .build();
        ChatResponse response = new ChatResponse(
                List.of(new Generation(new AssistantMessage("some response"))), metadata);
        when(orchestrator.chat("hi", "conv-1")).thenReturn(response);

        ChatReplyDto dto = chatService.chat("hi", "conv-1");

        verify(orchestrator).chat("hi", "conv-1");
        assertThat(dto.model()).isEqualTo("some-model");
        assertThat(dto.conversationId()).isEqualTo("conv-1");
        assertThat(dto.promptTokens()).isEqualTo(10);
        assertThat(dto.completionTokens()).isEqualTo(20);
        assertThat(dto.generations()).hasSize(1);
        assertThat(dto.generations().getFirst().text()).isEqualTo("some response");
    }
}