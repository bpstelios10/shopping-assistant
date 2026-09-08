package org.learnings.ai.shoppingassistant.services;

import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.agents.AgentChatResult;
import org.learnings.ai.shoppingassistant.agents.AgentOrchestrator;
import org.learnings.ai.shoppingassistant.agents.AgentStreamEvent;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.learnings.ai.shoppingassistant.services.dtos.ChatStreamEventDto;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplTest {

    private final AgentOrchestrator orchestrator = mock(AgentOrchestrator.class);
    private final ChatServiceImpl chatService = new ChatServiceImpl(orchestrator, Duration.ofSeconds(30));

    @Test
    void chat_delegatesToOrchestrator_andMapsResponse() {
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model("some-model")
                .usage(new DefaultUsage(10, 20))
                .build();
        ChatResponse chatResponse = new ChatResponse(
                List.of(new Generation(new AssistantMessage("some response"))), metadata);
        AgentChatResult response = AgentChatResult.builder().chatResponse(chatResponse).build();
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

    @Test
    void chatStream_mapsDomainEventsToDtos() {
        when(orchestrator.chatStream("hi", "conv-1")).thenReturn(Flux.just(
                new AgentStreamEvent.Routed("support", "conv-1", 0.9),
                new AgentStreamEvent.Delta("hel"),
                new AgentStreamEvent.Delta("lo"),
                new AgentStreamEvent.Completed("some-model", 10, 20)));

        StepVerifier.create(chatService.chatStream("hi", "conv-1"))
                .expectNext(new ChatStreamEventDto.Routed("support", "conv-1", 0.9))
                .expectNext(new ChatStreamEventDto.Delta("hel"))
                .expectNext(new ChatStreamEventDto.Delta("lo"))
                .expectNext(new ChatStreamEventDto.Done("some-model", 10, 20))
                .verifyComplete();
    }

    @Test
    void chatStream_whenUpstreamFails_completesNormallyWithErrorEvent() {
        when(orchestrator.chatStream("hi", "conv-1")).thenReturn(Flux.concat(
                Flux.just(new AgentStreamEvent.Delta("hi")),
                Flux.error(new IllegalStateException("model exploded"))));

        StepVerifier.create(chatService.chatStream("hi", "conv-1"))
                .expectNext(new ChatStreamEventDto.Delta("hi"))
                .expectNext(new ChatStreamEventDto.Error(ChatServiceImpl.FAILURE_MESSAGE))
                .verifyComplete();
    }

    @Test
    void chatStream_whenUpstreamFailsImmediately_emitsOnlyErrorEvent() {
        when(orchestrator.chatStream("hi", "conv-1"))
                .thenReturn(Flux.error(new IllegalStateException("router down")));

        StepVerifier.create(chatService.chatStream("hi", "conv-1"))
                .expectNext(new ChatStreamEventDto.Error(ChatServiceImpl.FAILURE_MESSAGE))
                .verifyComplete();
    }

    @Test
    void chatStream_whenUpstreamStalls_emitsTimeoutEvent() {
        ChatServiceImpl service = new ChatServiceImpl(orchestrator, Duration.ofMillis(50));
        when(orchestrator.chatStream("hi", "conv-1")).thenReturn(Flux.concat(
                Flux.just(new AgentStreamEvent.Delta("hi")),
                Flux.never()));

        StepVerifier.create(service.chatStream("hi", "conv-1"))
                .expectNext(new ChatStreamEventDto.Delta("hi"))
                .expectNext(new ChatStreamEventDto.Error(ChatServiceImpl.TIMEOUT_MESSAGE))
                .verifyComplete();
    }

    @Test
    void chatStream_isColdSoTheModelIsNotCalledUntilSubscribed() {
        AtomicBoolean subscribed = new AtomicBoolean();
        when(orchestrator.chatStream("hi", "conv-1")).thenReturn(
                Flux.<AgentStreamEvent>just(new AgentStreamEvent.Delta("hi"))
                        .doOnSubscribe(_ -> subscribed.set(true)));

        Flux<ChatStreamEventDto> stream = chatService.chatStream("hi", "conv-1");

        assertThat(subscribed).isFalse();
        stream.blockLast();
        assertThat(subscribed).isTrue();
    }

    @Test
    void chatStream_whenClientDisconnects_cancelsUpstream() {
        AtomicBoolean upstreamCancelled = new AtomicBoolean();
        when(orchestrator.chatStream("hi", "conv-1")).thenReturn(
                Flux.concat(
                                Flux.<AgentStreamEvent>just(new AgentStreamEvent.Delta("hi")),
                                Flux.never())
                        .doOnCancel(() -> upstreamCancelled.set(true)));

        StepVerifier.create(chatService.chatStream("hi", "conv-1"))
                .expectNext(new ChatStreamEventDto.Delta("hi"))
                .thenCancel()
                .verify();

        assertThat(upstreamCancelled).isTrue();
    }
}