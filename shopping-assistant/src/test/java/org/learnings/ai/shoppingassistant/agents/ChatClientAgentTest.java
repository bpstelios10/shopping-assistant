package org.learnings.ai.shoppingassistant.agents;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.agents.prompts.PromptProvider;
import org.learnings.ai.shoppingassistant.tools.ProductTool;
import org.learnings.ai.shoppingassistant.tools.ToolContextValues;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.client.advisor.api.MemoryAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.learnings.ai.shoppingassistant.advisors.ToolCallAuditingValues.TOOL_CALLS_CONTEXT_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatClientAgentTest {

    private static final String AGENT_NAME = "shopping";
    private static final String CONVERSATION_ID = "some-conversation-id";
    private static final String MESSAGE = "some message";
    private static final String USER_ID = "anon:sess-abc";
    private static final AgentRequest REQUEST = new AgentRequest(MESSAGE, CONVERSATION_ID, USER_ID);

    @Mock
    private ChatClient chatClient;
    @Mock
    private PromptProvider promptProvider;
    @Mock
    private ProductTool productTool;

    private ChatClientAgent agentWith(List<?> tools) {
        return new ChatClientAgent(AGENT_NAME, chatClient, promptProvider, tools);
    }

    @Test
    void name_returnsConfiguredName() {
        assertThat(agentWith(List.of()).name()).isEqualTo(AGENT_NAME);
    }

    @Test
    void chat_whenCorrectInput_returnsResponse() {
        ChatClient.ChatClientRequestSpec requestSpec = requestSpecReturning(true);
        ChatClient.CallResponseSpec callResponseSpec = mock(DefaultChatClient.DefaultCallResponseSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("some response"))));
        when(callResponseSpec.chatClientResponse()).thenReturn(new ChatClientResponse(chatResponse, Map.of()));

        AgentChatResult result = agentWith(List.of(productTool)).chat(REQUEST);
        ChatResponse response = result.chatResponse();

        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResult().getOutput().getText()).isEqualTo("some response");
        verifyNoMoreInteractions(chatClient, promptProvider, productTool, requestSpec, callResponseSpec);
    }

    @SuppressWarnings("unchecked")
    @Test
    void chat_whenNoTools_returnsResponse() {
        Prompt prompt = new Prompt(MESSAGE);
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(promptProvider.buildPrompt(MESSAGE, USER_ID)).thenReturn(prompt);
        when(chatClient.prompt(prompt)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.toolContext(any())).thenReturn(requestSpec);
        when(requestSpec.tools()).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(DefaultChatClient.DefaultCallResponseSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("some response"))));
        when(callResponseSpec.chatClientResponse()).thenReturn(new ChatClientResponse(chatResponse, Map.of()));

        AgentChatResult result = agentWith(List.of()).chat(REQUEST);

        assertThat(result.chatResponse().getResult()).isNotNull();
        assertThat(result.chatResponse().getResult().getOutput().getText()).isEqualTo("some response");
        verifyNoMoreInteractions(chatClient, promptProvider, requestSpec, callResponseSpec);
    }

    @Test
    void chat_whenClientThrows_throwsException() {
        ChatClient.ChatClientRequestSpec requestSpec = requestSpecReturning(false);
        when(requestSpec.call()).thenThrow(new RuntimeException("connection failed"));

        ChatClientAgent agent = agentWith(List.of(productTool));
        assertThatThrownBy(() -> agent.chat(REQUEST))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("connection failed");

        verifyNoMoreInteractions(chatClient, promptProvider, productTool, requestSpec);
    }

    @Test
    void chat_whenNoResponse_throwsException() {
        ChatClient.ChatClientRequestSpec requestSpec = requestSpecReturning(false);
        ChatClient.CallResponseSpec callResponseSpec = mock(DefaultChatClient.DefaultCallResponseSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatClientResponse()).thenReturn(new ChatClientResponse(null, Map.of()));

        ChatClientAgent agent = agentWith(List.of(productTool));
        assertThatThrownBy(() -> agent.chat(REQUEST))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Agent [" + AGENT_NAME + "] did not respond with a chat response");

        verifyNoMoreInteractions(chatClient, promptProvider, productTool, requestSpec, callResponseSpec);
    }

    @Test
    void chat_passesUserIdThroughToolContext() {
        ChatClient.ChatClientRequestSpec requestSpec = requestSpecReturning(false);
        ChatClient.CallResponseSpec callResponseSpec = mock(DefaultChatClient.DefaultCallResponseSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("hi"))));
        when(callResponseSpec.chatClientResponse()).thenReturn(new ChatClientResponse(chatResponse, Map.of()));

        agentWith(List.of(productTool)).chat(REQUEST);

        verify(requestSpec).toolContext(Map.of(ToolContextValues.USER_ID, USER_ID));
    }

    @Test
    void chatStream_emitsDeltasThenCompleted() {
        ChatClient.StreamResponseSpec streamSpec = streamSpecReturning(Flux.just(
                chunk("Hel", null),
                chunk("lo", null),
                chunk("", metadata())));

        StepVerifier.create(agentWith(List.of(productTool)).chatStream(REQUEST))
                .expectNext(new AgentStreamEvent.Delta("Hel"))
                .expectNext(new AgentStreamEvent.Delta("lo"))
                .expectNext(new AgentStreamEvent.Completed("some-model", 10, 20))
                .verifyComplete();

        verify(streamSpec).chatClientResponse();
    }

    @Test
    void chatStream_emitsToolInvokedBeforeText() {
        AssistantMessage.ToolCall toolCall =
                new AssistantMessage.ToolCall("id-1", "function", "searchProducts", "{}");
        AssistantMessage withTool = AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build();
        streamSpecReturning(Flux.just(
                new ChatClientResponse(new ChatResponse(List.of(new Generation(withTool))), Map.of()),
                chunk("found it", metadata())));

        StepVerifier.create(agentWith(List.of(productTool)).chatStream(REQUEST))
                .expectNext(new AgentStreamEvent.ToolInvoked(toolCall))
                .expectNext(new AgentStreamEvent.Delta("found it"))
                .expectNext(new AgentStreamEvent.Completed("some-model", 10, 20))
                .verifyComplete();
    }

    @Test
    void chatStream_whenNoMetadataEverSent_completesWithNulls() {
        streamSpecReturning(Flux.just(chunk("hi", null)));

        StepVerifier.create(agentWith(List.of(productTool)).chatStream(REQUEST))
                .expectNext(new AgentStreamEvent.Delta("hi"))
                .expectNext(new AgentStreamEvent.Completed(null, null, null))
                .verifyComplete();
    }

    @Test
    void chatStream_skipsEmptyAndResultlessChunks() {
        streamSpecReturning(Flux.just(
                new ChatClientResponse(null, Map.of()),
                new ChatClientResponse(new ChatResponse(List.of()), Map.of()),
                chunk(null, null),
                chunk("", null),
                chunk("hi", null)));

        StepVerifier.create(agentWith(List.of(productTool)).chatStream(REQUEST))
                .expectNext(new AgentStreamEvent.Delta("hi"))
                .expectNext(new AgentStreamEvent.Completed(null, null, null))
                .verifyComplete();
    }

    @Test
    void chatStream_propagatesModelErrors() {
        streamSpecReturning(Flux.concat(
                Flux.just(chunk("hi", null)),
                Flux.error(new IllegalStateException("model exploded"))));

        StepVerifier.create(agentWith(List.of(productTool)).chatStream(REQUEST))
                .expectNext(new AgentStreamEvent.Delta("hi"))
                .verifyErrorMessage("model exploded");
    }

    @Test
    void chatStream_isColdAndBuildsPromptPerSubscription() {
        streamSpecReturning(Flux.just(chunk("hi", null)));

        Flux<AgentStreamEvent> stream = agentWith(List.of(productTool)).chatStream(REQUEST);
        verifyNoMoreInteractions(promptProvider);

        stream.blockLast();
        stream.blockLast();

        verify(promptProvider, org.mockito.Mockito.times(2)).buildPrompt(MESSAGE, USER_ID);
    }

    @Test
    void chatStream_whenMetadataCarriesNoUsage_completesWithModelOnly() {
        ChatResponseMetadata responseMetadata = mock(ChatResponseMetadata.class);
        when(responseMetadata.getModel()).thenReturn("qwen3:8b");
        Usage usage = mock(Usage.class);
        when(responseMetadata.getUsage()).thenReturn(usage);
        streamSpecReturning(Flux.just(chunk("hi", responseMetadata)));

        StepVerifier.create(agentWith(List.of(productTool)).chatStream(REQUEST))
                .expectNext(new AgentStreamEvent.Delta("hi"))
                .expectNext(new AgentStreamEvent.Completed("qwen3:8b", null, null))
                .verifyComplete();
    }

    @Test
    void chatStream_whenUsageHasNoTotalTokens_treatsUsageAsMissing() {
        Usage emptyUsage = mock(Usage.class);
        when(emptyUsage.getTotalTokens()).thenReturn(null);
        ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
        when(metadata.getModel()).thenReturn("");
        when(metadata.getUsage()).thenReturn(emptyUsage);
        streamSpecReturning(Flux.just(chunk("hi", metadata)));

        StepVerifier.create(agentWith(List.of(productTool)).chatStream(REQUEST))
                .expectNext(new AgentStreamEvent.Delta("hi"))
                .expectNext(new AgentStreamEvent.Completed(null, null, null))
                .verifyComplete();
    }

    @Test
    void chatStream_whenLaterChunksHaveNone_keepsTheChunkThatCarriedUsage() {
        ChatResponseMetadata responseMetadata = mock(ChatResponseMetadata.class);
        Usage usage = mock(Usage.class);
        when(responseMetadata.getUsage()).thenReturn(usage);
        streamSpecReturning(Flux.just(
                chunk("a", metadata()),
                chunk("b", responseMetadata)));

        StepVerifier.create(agentWith(List.of(productTool)).chatStream(REQUEST))
                .expectNext(new AgentStreamEvent.Delta("a"))
                .expectNext(new AgentStreamEvent.Delta("b"))
                .expectNext(new AgentStreamEvent.Completed("some-model", 10, 20))
                .verifyComplete();
    }

    @Test
    void chatStream_whenNoChunksAtAll_emitsOnlyCompleted() {
        streamSpecReturning(Flux.empty());

        StepVerifier.create(agentWith(List.of(productTool)).chatStream(REQUEST))
                .expectNext(new AgentStreamEvent.Completed(null, null, null))
                .verifyComplete();
    }

    @SuppressWarnings("unchecked")
    private ChatClient.ChatClientRequestSpec requestSpecReturning(boolean verifyAdvisorParams) {
        Prompt prompt = new Prompt(MESSAGE);
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(promptProvider.buildPrompt(MESSAGE, USER_ID)).thenReturn(prompt);
        when(chatClient.prompt(prompt)).thenReturn(requestSpec);

        if (verifyAdvisorParams) {
            doAnswer(invocation -> {
                Consumer<ChatClient.AdvisorSpec> consumer = invocation.getArgument(0);
                ChatClient.AdvisorSpec advisorSpec = mock(ChatClient.AdvisorSpec.class);
                when(advisorSpec.param(any(), any())).thenReturn(advisorSpec);
                consumer.accept(advisorSpec);
                verify(advisorSpec).param(MemoryAdvisor.CONVERSATION_ID, CONVERSATION_ID);
                verify(advisorSpec).param("agent", AGENT_NAME);
                verify(advisorSpec).param(eq(TOOL_CALLS_CONTEXT_KEY), anyList());

                return requestSpec;
            }).when(requestSpec).advisors(any(Consumer.class));
        } else {
            when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        }

        when(requestSpec.toolContext(any())).thenReturn(requestSpec);
        when(requestSpec.tools(productTool)).thenReturn(requestSpec);

        return requestSpec;
    }

    private ChatClient.StreamResponseSpec streamSpecReturning(Flux<ChatClientResponse> responses) {
        ChatClient.ChatClientRequestSpec requestSpec = requestSpecReturning(false);
        ChatClient.StreamResponseSpec streamSpec = mock(DefaultChatClient.DefaultStreamResponseSpec.class);
        when(requestSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.chatClientResponse()).thenReturn(responses);

        return streamSpec;
    }

    private ChatClientResponse chunk(String text, ChatResponseMetadata metadata) {
        ChatResponse chatResponse = metadata == null
                ? new ChatResponse(List.of(new Generation(new AssistantMessage(text))))
                : new ChatResponse(List.of(new Generation(new AssistantMessage(text))), metadata);

        return new ChatClientResponse(chatResponse, Map.of());
    }

    private ChatResponseMetadata metadata() {
        return ChatResponseMetadata.builder()
                .model("some-model")
                .usage(new DefaultUsage(10, 20))
                .build();
    }
}
