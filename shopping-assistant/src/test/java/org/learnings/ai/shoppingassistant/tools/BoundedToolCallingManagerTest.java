package org.learnings.ai.shoppingassistant.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoundedToolCallingManagerTest {

    private static final int MAX_ITERATIONS = 2;

    @Mock
    private ToolCallingManager delegate;
    private BoundedToolCallingManager manager;

    @BeforeEach
    void setup() {
        manager = new BoundedToolCallingManager(delegate, MAX_ITERATIONS);
    }

    @Test
    void constructor_whenMaxIterationsBelowOne_throws() {
        assertThatThrownBy(() -> new BoundedToolCallingManager(delegate, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxIterations must be at least 1, but was 0");
    }

    @Test
    void resolveToolDefinitions_delegates() {
        ToolCallingChatOptions options = ToolCallingChatOptions.builder().build();
        List<ToolDefinition> definitions = List.of();
        when(delegate.resolveToolDefinitions(options)).thenReturn(definitions);

        List<ToolDefinition> actual = manager.resolveToolDefinitions(options);

        assertThat(actual).isSameAs(definitions);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    void executeToolCalls_whenBelowLimit_delegates() {
        Prompt prompt = promptWithCompletedIterations(1);
        ChatResponse response = responseWithToolCall();
        ToolExecutionResult expected = ToolExecutionResult.builder()
                .conversationHistory(List.of())
                .returnDirect(false)
                .build();
        when(delegate.executeToolCalls(prompt, response)).thenReturn(expected);

        ToolExecutionResult actual = manager.executeToolCalls(prompt, response);

        assertThat(actual).isSameAs(expected);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    void executeToolCalls_whenLimitReached_nudgesModelWithoutExecutingTools() {
        Prompt prompt = promptWithCompletedIterations(MAX_ITERATIONS);
        ChatResponse response = responseWithToolCall();

        ToolExecutionResult result = manager.executeToolCalls(prompt, response);

        assertThat(result.returnDirect()).isFalse();
        assertThat(lastToolResponseText(result)).isEqualTo(BoundedToolCallingManager.LIMIT_REACHED_MESSAGE);
        verifyNoInteractions(delegate);
    }

    @Test
    void executeToolCalls_whenNudgeIgnored_hardStopsTheLoop() {
        Prompt prompt = promptWithCompletedIterations(MAX_ITERATIONS + 1);
        ChatResponse response = responseWithToolCall();

        ToolExecutionResult result = manager.executeToolCalls(prompt, response);

        assertThat(result.returnDirect()).isTrue();
        assertThat(lastToolResponseText(result)).isEqualTo(BoundedToolCallingManager.HARD_STOP_MESSAGE);
        verifyNoInteractions(delegate);
    }

    @Test
    void executeToolCalls_whenNudgeIgnoredAndResponseNoResults_delegates() {
        Prompt prompt = promptWithCompletedIterations(MAX_ITERATIONS + 1);
        ChatResponse response = new ChatResponse(List.of());
        ToolExecutionResult expected = ToolExecutionResult.builder()
                .conversationHistory(List.of())
                .returnDirect(false)
                .build();
        when(delegate.executeToolCalls(prompt, response)).thenReturn(expected);

        ToolExecutionResult result = manager.executeToolCalls(prompt, response);

        assertThat(result).isSameAs(expected);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    void executeToolCalls_whenNudgeIgnoredAndResponseHasNoTools_delegates() {
        Prompt prompt = promptWithCompletedIterations(MAX_ITERATIONS + 1);
        ChatResponse response = responseWithToolCalls();
        ToolExecutionResult expected = ToolExecutionResult.builder()
                .conversationHistory(List.of())
                .returnDirect(false)
                .build();
        when(delegate.executeToolCalls(prompt, response)).thenReturn(expected);

        ToolExecutionResult result = manager.executeToolCalls(prompt, response);

        assertThat(result).isSameAs(expected);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    void executeToolCalls_whenTerminating_answersEveryRequestedToolCall() {
        Prompt prompt = promptWithCompletedIterations(MAX_ITERATIONS);
        ChatResponse response = responseWithToolCalls(
                new AssistantMessage.ToolCall("id-1", "function", "searchProducts", "{}"),
                new AssistantMessage.ToolCall("id-2", "function", "listCategories", "{}"));

        ToolExecutionResult result = manager.executeToolCalls(prompt, response);

        ToolResponseMessage toolResponseMessage = lastToolResponseMessage(result);
        assertThat(toolResponseMessage.getResponses())
                .extracting(ToolResponseMessage.ToolResponse::id)
                .containsExactly("id-1", "id-2");
        assertThat(toolResponseMessage.getResponses())
                .extracting(ToolResponseMessage.ToolResponse::name)
                .containsExactly("searchProducts", "listCategories");
        // history keeps the original messages, then the assistant message, then the synthetic tool responses
        assertThat(result.conversationHistory()).hasSize(prompt.getInstructions().size() + 2);
        verifyNoInteractions(delegate);
    }

    private Prompt promptWithCompletedIterations(int completedIterations) {
        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage("find me running shoes"));
        for (int i = 0; i < completedIterations; i++) {
            messages.add(ToolResponseMessage.builder()
                    .responses(List.of(new ToolResponseMessage.ToolResponse("id-" + i, "searchProducts", "[]")))
                    .build());
        }
        return new Prompt(messages);
    }

    private ChatResponse responseWithToolCall() {
        return responseWithToolCalls(new AssistantMessage.ToolCall("id-1", "function", "searchProducts", "{}"));
    }

    private ChatResponse responseWithToolCalls(AssistantMessage.ToolCall... toolCalls) {
        AssistantMessage assistantMessage =
                AssistantMessage.builder().content("").toolCalls(List.of(toolCalls)).build();
        return new ChatResponse(List.of(new Generation(assistantMessage)));
    }

    private ToolResponseMessage lastToolResponseMessage(ToolExecutionResult result) {
        List<Message> history = result.conversationHistory();
        return (ToolResponseMessage) history.getLast();
    }

    private String lastToolResponseText(ToolExecutionResult result) {
        return lastToolResponseMessage(result).getResponses().getFirst().responseData();
    }
}
