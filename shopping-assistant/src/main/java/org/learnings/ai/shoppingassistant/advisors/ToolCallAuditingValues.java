package org.learnings.ai.shoppingassistant.advisors;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public final class ToolCallAuditingValues {

    public static final String TOOL_CALLS_CONTEXT_KEY = "toolCalls";

    private ToolCallAuditingValues() { }

    public static List<AssistantMessage.ToolCall> getToolCalls(Map<String, Object> context) {
        Object value = context.get(TOOL_CALLS_CONTEXT_KEY);
        if (!(value instanceof List<?> raw)) return List.of();

        List<AssistantMessage.ToolCall> typed = new ArrayList<>(raw.size());
        for (Object item : raw) {
            if (item instanceof AssistantMessage.ToolCall tc) typed.add(tc);
        }
        return List.copyOf(typed);
    }

    public static void appendToolCalls(Map<String, Object> context, List<AssistantMessage.ToolCall> toAdd) {
        Object value = context.get(TOOL_CALLS_CONTEXT_KEY);
        if (!(value instanceof List<?> raw)) return;

        if (!raw.stream().allMatch(AssistantMessage.ToolCall.class::isInstance)) {
            log.warn("Skipping toolCalls context update: list contains non-ToolCall elements");
            return;
        }

        @SuppressWarnings("unchecked")
        List<AssistantMessage.ToolCall> typed = (List<AssistantMessage.ToolCall>) raw;
        try {
            typed.addAll(toAdd);
        } catch (UnsupportedOperationException e) {
            log.warn("Skipping toolCalls context update: context list is immutable", e);
        }
    }
}
