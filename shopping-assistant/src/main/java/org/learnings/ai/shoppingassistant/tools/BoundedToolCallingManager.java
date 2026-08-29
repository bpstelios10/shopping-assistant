package org.learnings.ai.shoppingassistant.tools;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Caps how many tool-calling round-trips a single user turn may trigger.
 *
 * <p>Spring AI's tool loop (model → tool calls → model → ...) has no built-in iteration limit: it ends only when
 * the model stops emitting tool calls. A model that oscillates between two tools therefore burns tokens, wall-clock
 * and downstream calls until something else gives out — and each round costs more than the last, because the
 * conversation history keeps growing.
 *
 * <p>The iteration count is derived from the prompt itself: every completed round leaves one
 * {@link ToolResponseMessage} in the message list, so no state is kept between calls and the decorator is
 * thread-safe.
 *
 * <p>Two stages, so that a breach degrades instead of failing:
 * <ol>
 *     <li>at {@code maxIterations} — tools are not executed and the model is told to answer with what it already
 *         has ({@code returnDirect = false}), giving it one round to produce a proper reply;</li>
 *     <li>at {@code maxIterations + 1} — the nudge was ignored, so the loop is terminated unconditionally
 *         ({@code returnDirect = true}) and the fallback text is returned to the shopper.</li>
 * </ol>
 *
 * <p>A breach is never turned into an exception: the shopper has already paid for several inferences by then, so a
 * degraded answer beats a 500. Same philosophy as the {@code ToolExecutionExceptionProcessor}.
 */
@Slf4j
public class BoundedToolCallingManager implements ToolCallingManager {

    static final String LIMIT_REACHED_MESSAGE =
            "Tool call limit reached. Answer the shopper now using the information you already have, "
                    + "and tell them if something could not be retrieved.";

    static final String HARD_STOP_MESSAGE =
            "I couldn't complete that request. Could you rephrase it or give me a bit more detail?";

    private final ToolCallingManager delegate;
    private final int maxIterations;

    public BoundedToolCallingManager(ToolCallingManager delegate, int maxIterations) {
        if (maxIterations < 1) {
            throw new IllegalArgumentException("maxIterations must be at least 1, but was " + maxIterations);
        }
        this.delegate = delegate;
        this.maxIterations = maxIterations;
    }

    @Override
    public @NonNull List<ToolDefinition> resolveToolDefinitions(@NonNull ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public @NonNull ToolExecutionResult executeToolCalls(@NonNull Prompt prompt, @NonNull ChatResponse response) {
        long iterations = countCompletedIterations(prompt);

        if (iterations >= maxIterations + 1L) {
            log.warn("tool call loop hard-stopped after [{}] iterations (max [{}])", iterations, maxIterations);
            return terminate(prompt, response, HARD_STOP_MESSAGE, true);
        }

        if (iterations >= maxIterations) {
            log.warn("tool call limit of [{}] reached, asking the model to answer with what it has", maxIterations);
            return terminate(prompt, response, LIMIT_REACHED_MESSAGE, false);
        }

        return delegate.executeToolCalls(prompt, response);
    }

    private long countCompletedIterations(Prompt prompt) {
        return prompt.getInstructions().stream()
                .filter(ToolResponseMessage.class::isInstance)
                .count();
    }

    /**
     * Answers every requested tool call with {@code message} without executing any of them. Each tool call must get
     * a response, otherwise the provider rejects the next request.
     */
    private ToolExecutionResult terminate(Prompt prompt, ChatResponse response, String message, boolean returnDirect) {
        if (response.getResult() == null || !response.hasToolCalls()) {
            // practically unreachable... IF it was, then nothing to bound. Delegate
            return delegate.executeToolCalls(prompt, response);
        }
        AssistantMessage assistantMessage = response.getResult().getOutput();

        List<ToolResponseMessage.ToolResponse> toolResponses = assistantMessage.getToolCalls().stream()
                .map(toolCall -> new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), message))
                .toList();

        List<Message> history = new ArrayList<>(prompt.getInstructions());
        history.add(assistantMessage);
        history.add(ToolResponseMessage.builder().responses(toolResponses).build());

        return ToolExecutionResult.builder()
                .conversationHistory(history)
                .returnDirect(returnDirect)
                .build();
    }
}
