package org.learnings.ai.shoppingassistant.advisors;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * the {@code "toolCalls"} key in the advisor request context.
 *
 * <p>Callers must pre-populate the context with a mutable list (e.g. via
 * {@code .param("toolCalls", new ArrayList<>())}) before invoking the chat client;
 * if the key is missing, not a list, contains non-{@code ToolCall} elements, or is
 * immutable, the update is skipped (and logged) without failing the call.
 */
@Component
public class ToolCallAuditingAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(ToolCallAuditingAdvisor.class);

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
            addToolCallsToResponseContext(response, toolCalls);
        }

        return response;
    }

    @Override
    public @NonNull String getName() {
        return "Tool Calls Auditor Advisor";
    }

    private static void addToolCallsToResponseContext(ChatClientResponse response, List<AssistantMessage.ToolCall> toolCalls) {
        Object value = response.context().get("toolCalls");
        if (value instanceof List<?> rawList) {
            boolean allToolCalls = rawList.stream().allMatch(AssistantMessage.ToolCall.class::isInstance);
            if (allToolCalls) {
                @SuppressWarnings("unchecked")
                List<AssistantMessage.ToolCall> contextToolCalls = (List<AssistantMessage.ToolCall>) rawList;
                try {
                    contextToolCalls.addAll(toolCalls);
                } catch (UnsupportedOperationException e) {
                    log.warn("Skipping toolCalls context update: context list is immutable", e);
                }
            } else {
                log.warn("Skipping toolCalls context update: list contains non-ToolCall elements");
            }
        }
    }
}
