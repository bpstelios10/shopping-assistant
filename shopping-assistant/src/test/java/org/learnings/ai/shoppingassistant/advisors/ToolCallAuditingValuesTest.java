package org.learnings.ai.shoppingassistant.advisors;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.learnings.ai.shoppingassistant.advisors.ToolCallAuditingValues.TOOL_CALLS_CONTEXT_KEY;
import static org.mockito.Mockito.*;

class ToolCallAuditingValuesTest {

    @Test
    void appendToolCalls_whenKeyMissing_doesNothing() {
        Map<String, Object> context = new HashMap<>();
        AssistantMessage.ToolCall toolCall = mock(AssistantMessage.ToolCall.class);

        ToolCallAuditingValues.appendToolCalls(context, List.of(toolCall));

        assertThat(context).isEmpty();
    }

    @Test
    void appendToolCalls_whenValueNotList_doesNothing() {
        Map<String, Object> context = new HashMap<>();
        context.put(TOOL_CALLS_CONTEXT_KEY, "not-a-list");
        AssistantMessage.ToolCall toolCall = mock(AssistantMessage.ToolCall.class);

        ToolCallAuditingValues.appendToolCalls(context, List.of(toolCall));

        assertThat(context).containsOnlyKeys(TOOL_CALLS_CONTEXT_KEY);
        assertThat(context.get(TOOL_CALLS_CONTEXT_KEY)).isEqualTo("not-a-list");
    }

    @Test
    void appendToolCalls_whenListContainsNonToolCall_logsWarnAndSkips() {
        Map<String, Object> context = new HashMap<>();
        List<Object> mixed = new ArrayList<>();
        mixed.add("wrong");
        context.put(TOOL_CALLS_CONTEXT_KEY, mixed);

        AssistantMessage.ToolCall toolCall = mock(AssistantMessage.ToolCall.class);

        ToolCallAuditingValues.appendToolCalls(context, List.of(toolCall));

        assertThat(context).containsOnlyKeys(TOOL_CALLS_CONTEXT_KEY);
        List<?> actual = (List<?>) context.get(TOOL_CALLS_CONTEXT_KEY);
        assertThat(actual).hasSize(1);
        assertThat(actual.getFirst()).isEqualTo("wrong");
    }

    @Test
    void appendToolCalls_whenListImmutable_logsWarnAndSkips() {
        AssistantMessage.ToolCall existing = mock(AssistantMessage.ToolCall.class);
        AssistantMessage.ToolCall toAdd = mock(AssistantMessage.ToolCall.class);

        Map<String, Object> context = new HashMap<>();
        context.put(TOOL_CALLS_CONTEXT_KEY, List.of(existing));

        ToolCallAuditingValues.appendToolCalls(context, List.of(toAdd));

        assertThat(context).containsOnlyKeys(TOOL_CALLS_CONTEXT_KEY);
        List<?> actual = (List<?>) context.get(TOOL_CALLS_CONTEXT_KEY);
        assertThat(actual).hasSize(1);
        assertThat(actual.getFirst()).isEqualTo(existing);
    }

    @SuppressWarnings("unchecked")
    @Test
    void appendToolCalls_whenValidMutableList_appendsAll() {
        AssistantMessage.ToolCall existing = mock(AssistantMessage.ToolCall.class);
        AssistantMessage.ToolCall tc1 = mock(AssistantMessage.ToolCall.class);
        AssistantMessage.ToolCall tc2 = mock(AssistantMessage.ToolCall.class);

        List<AssistantMessage.ToolCall> list = new ArrayList<>();
        list.add(existing);

        Map<String, Object> context = new HashMap<>();
        context.put(TOOL_CALLS_CONTEXT_KEY, list);

        ToolCallAuditingValues.appendToolCalls(context, List.of(tc1, tc2));

        assertThat(context).containsOnlyKeys(TOOL_CALLS_CONTEXT_KEY);
        List<AssistantMessage.ToolCall> actual = (List<AssistantMessage.ToolCall>) context.get(TOOL_CALLS_CONTEXT_KEY);
        assertThat(actual).containsExactly(existing, tc1, tc2);
    }

    @Test
    void getToolCalls_whenKeyMissing_returnsEmpty() {
        Map<String, Object> context = new HashMap<>();

        List<AssistantMessage.ToolCall> result = ToolCallAuditingValues.getToolCalls(context);

        assertThat(result).isEmpty();
    }

    @Test
    void getToolCalls_whenNotList_returnsEmpty() {
        Map<String, Object> context = new HashMap<>();
        context.put(TOOL_CALLS_CONTEXT_KEY, "x");

        List<AssistantMessage.ToolCall> result = ToolCallAuditingValues.getToolCalls(context);

        assertThat(result).isEmpty();
    }

    @Test
    void getToolCalls_whenMixedTypes_returnsOnlyToolCalls() {
        AssistantMessage.ToolCall tc = mock(AssistantMessage.ToolCall.class);
        Map<String, Object> context = new HashMap<>();
        context.put(TOOL_CALLS_CONTEXT_KEY, List.of("x", tc, 1));

        List<AssistantMessage.ToolCall> result = ToolCallAuditingValues.getToolCalls(context);

        assertThat(result).containsExactly(tc);
    }

    @Test
    void getToolCalls_whenValidList_returnsSnapshot() {
        AssistantMessage.ToolCall tc = mock(AssistantMessage.ToolCall.class);
        List<AssistantMessage.ToolCall> source = new ArrayList<>();
        source.add(tc);

        Map<String, Object> context = new HashMap<>();
        context.put(TOOL_CALLS_CONTEXT_KEY, source);

        List<AssistantMessage.ToolCall> result = ToolCallAuditingValues.getToolCalls(context);

        assertThat(result).containsExactly(tc);
        assertThat(result).isNotSameAs(source); // snapshot
    }
}
