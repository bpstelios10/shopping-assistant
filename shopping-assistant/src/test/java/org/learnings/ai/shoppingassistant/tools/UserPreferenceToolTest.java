package org.learnings.ai.shoppingassistant.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryService;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserPreferenceToolTest {

    @Mock
    private UserMemoryService userMemoryService;

    @InjectMocks
    private UserPreferenceTool tool;

    @Test
    void saveUserPreference_whenUserInToolContext_persistsPreference() {
        String result = tool.saveUserPreference("currency", "EUR", toolContext("user-1"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(userMemoryService).updateProfile(eq("user-1"), captor.capture());
        assertThat(captor.getValue()).containsEntry("currency", "EUR");
        assertThat(result).contains("currency=EUR");
    }

    @Test
    void saveUserPreference_whenToolContextIsNull_doesNotPersistAndReturnsMessage() {
        String result = tool.saveUserPreference("currency", "EUR", null);

        verifyNoInteractions(userMemoryService);
        assertThat(result).contains("no user context");
    }

    @Test
    void saveUserPreference_whenToolContextHasNoUser_doesNotPersistAndReturnsMessage() {
        String result = tool.saveUserPreference("currency", "EUR", new ToolContext(Map.of("other", "value")));

        verifyNoInteractions(userMemoryService);
        assertThat(result).contains("no user context");
    }

    @Test
    void saveUserPreference_whenUserIsBlank_doesNotPersistAndReturnsMessage() {
        String result = tool.saveUserPreference("currency", "EUR", toolContext("  "));

        verifyNoInteractions(userMemoryService);
        assertThat(result).contains("no user context");
    }

    @Test
    void saveUserPreference_whenUserIsNotAString_doesNotPersistAndReturnsMessage() {
        Map<String, Object> context = new HashMap<>();
        context.put(ToolContextValues.USER_ID, 42);

        String result = tool.saveUserPreference("currency", "EUR", new ToolContext(context));

        verifyNoInteractions(userMemoryService);
        assertThat(result).contains("no user context");
    }

    private ToolContext toolContext(String userId) {
        return new ToolContext(ToolContextValues.forUser(userId));
    }
}
