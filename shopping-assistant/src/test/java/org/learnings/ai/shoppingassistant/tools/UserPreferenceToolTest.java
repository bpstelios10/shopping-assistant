package org.learnings.ai.shoppingassistant.tools;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.services.memory.CurrentUser;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryService;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    @Test
    void saveUserPreference_whenUserPresent_persistsPreference() {
        CurrentUser.set("user-1");

        String result = tool.saveUserPreference("currency", "EUR");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(userMemoryService).updateProfile(eq("user-1"), captor.capture());
        assertThat(captor.getValue()).containsEntry("currency", "EUR");
        assertThat(result).contains("currency=EUR");
    }

    @Test
    void saveUserPreference_whenNoUser_doesNotPersistAndReturnsMessage() {
        // no CurrentUser set
        String result = tool.saveUserPreference("currency", "EUR");

        verifyNoInteractions(userMemoryService);
        assertThat(result).contains("no user context");
    }
}
