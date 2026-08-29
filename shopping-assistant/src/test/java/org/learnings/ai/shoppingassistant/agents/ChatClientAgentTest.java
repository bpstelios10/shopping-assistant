package org.learnings.ai.shoppingassistant.agents;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.agents.prompts.PromptProvider;
import org.learnings.ai.shoppingassistant.tools.ProductTool;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.learnings.ai.shoppingassistant.advisors.ToolCallAuditingValues.TOOL_CALLS_CONTEXT_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatClientAgentTest {

    private static final String AGENT_NAME = "shopping";
    private static final String CONVERSATION_ID = "some-conversation-id";
    private static final String MESSAGE = "some message";

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

    @SuppressWarnings("unchecked")
    @Test
    void chat_whenCorrectInput_returnsResponse() {
        Prompt prompt = new Prompt(MESSAGE);
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(promptProvider.buildPrompt(eq(MESSAGE))).thenReturn(prompt);
        when(chatClient.prompt(prompt)).thenReturn(requestSpec);
        // use doAnswer one time to verify that the advisor params are correct
        doAnswer(invocation -> {
            Consumer<ChatClient.AdvisorSpec> consumer = invocation.getArgument(0);
            ChatClient.AdvisorSpec advisorSpec = mock(ChatClient.AdvisorSpec.class);
            when(advisorSpec.param(any(), any())).thenReturn(advisorSpec);
            consumer.accept(advisorSpec);
            verify(advisorSpec).param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID);
            verify(advisorSpec).param("agent", AGENT_NAME);
            verify(advisorSpec).param(eq(TOOL_CALLS_CONTEXT_KEY), anyList());

            return requestSpec;
        }).when(requestSpec).advisors(any(Consumer.class));
        when(requestSpec.tools(productTool)).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(DefaultChatClient.DefaultCallResponseSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("some response"))));
        when(callResponseSpec.chatClientResponse()).thenReturn(new ChatClientResponse(chatResponse, Map.of()));

        AgentChatResult result = agentWith(List.of(productTool)).chat(MESSAGE, CONVERSATION_ID);
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
        when(promptProvider.buildPrompt(eq(MESSAGE))).thenReturn(prompt);
        when(chatClient.prompt(prompt)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.tools()).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(DefaultChatClient.DefaultCallResponseSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("some response"))));
        when(callResponseSpec.chatClientResponse()).thenReturn(new ChatClientResponse(chatResponse, Map.of()));

        AgentChatResult result = agentWith(List.of()).chat(MESSAGE, CONVERSATION_ID);

        assertThat(result.chatResponse().getResult()).isNotNull();
        assertThat(result.chatResponse().getResult().getOutput().getText()).isEqualTo("some response");
        verifyNoMoreInteractions(chatClient, promptProvider, requestSpec, callResponseSpec);
    }

    @SuppressWarnings("unchecked")
    @Test
    void chat_whenClientThrows_throwsException() {
        Prompt prompt = new Prompt(MESSAGE);
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(promptProvider.buildPrompt(eq(MESSAGE))).thenReturn(prompt);
        when(chatClient.prompt(prompt)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.tools(productTool)).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("connection failed"));

        ChatClientAgent agent = agentWith(List.of(productTool));
        assertThatThrownBy(() -> agent.chat(MESSAGE, CONVERSATION_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("connection failed");

        verifyNoMoreInteractions(chatClient, promptProvider, productTool, requestSpec);
    }

    @SuppressWarnings("unchecked")
    @Test
    void chat_whenNoResponse_throwsException() {
        Prompt prompt = new Prompt(MESSAGE);
        ChatClient.ChatClientRequestSpec requestSpec = mock(DefaultChatClient.DefaultChatClientRequestSpec.class);
        when(promptProvider.buildPrompt(eq(MESSAGE))).thenReturn(prompt);
        when(chatClient.prompt(prompt)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.tools(productTool)).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(DefaultChatClient.DefaultCallResponseSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatClientResponse()).thenReturn(new ChatClientResponse(null, Map.of()));

        ChatClientAgent agent = agentWith(List.of(productTool));
        assertThatThrownBy(() -> agent.chat(MESSAGE, CONVERSATION_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Agent [" + AGENT_NAME + "] did not respond with a chat response");

        verifyNoMoreInteractions(chatClient, promptProvider, productTool, requestSpec, callResponseSpec);
    }
}
