package org.learnings.ai.shoppingassistant.advisors;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Post-tool-calling advisor that captures executed tool calls from the model response
 * and appends them to the mutable {@code List<AssistantMessage.ToolCall>} stored under
 * the {@code TOOL_CALLS_CONTEXT_KEY} key in the advisor request context.
 *
 * <p>Callers must pre-populate the context with a mutable list (e.g. via
 * {@code .param(TOOL_CALLS_CONTEXT_KEY, new ArrayList<>())}) before invoking the chat client;
 * if the key is missing, not a list, contains non-{@code ToolCall} elements, or is
 * immutable, the update is skipped (and logged) without failing the call.
 */
@Slf4j
@Component
public class ToolCallAuditingAdvisor implements CallAdvisor {

    /**
     * {@link ToolCallingAdvisor} has order = Ordered.HIGHEST_PRECEDENCE + 300
     *
     * @return an order that is just right after the ToolCallingAdvisor
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 400;
    }

    @Override
    public @NonNull ChatClientResponse adviseCall(@NonNull ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);
        ChatResponse chatResponse = response.chatResponse();

        if (chatResponse != null && chatResponse.getResult() != null && chatResponse.hasToolCalls()) {
            List<AssistantMessage.ToolCall> toolCalls = chatResponse.getResult().getOutput().getToolCalls();
            log.debug("Tool calls: {}", toolCalls);
            ToolCallAuditingValues.appendToolCalls(response.context(), toolCalls);
        }

        return response;
    }

    @Override
    public @NonNull String getName() {
        return "Tool Calls Auditor Advisor";
    }
}
