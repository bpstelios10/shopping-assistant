package org.learnings.ai.shoppingassistant.tools;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToolContextValuesTest {

    @Test
    void forUser_returnsMapWithUserId() {
        assertThat(ToolContextValues.forUser("anon:sess-abc"))
                .containsEntry(ToolContextValues.USER_ID, "anon:sess-abc");
    }

    @Test
    void userId_whenContextIsNull_returnsEmpty() {
        assertThat(ToolContextValues.userId(null)).isEmpty();
    }

    @Test
    void userId_whenUserIdMissing_returnsEmpty() {
        ToolContext toolContext = new ToolContext(Map.of());

        assertThat(ToolContextValues.userId(toolContext)).isEmpty();
    }

    @Test
    void userId_whenUserIdIsNotAString_returnsEmpty() {
        ToolContext toolContext = new ToolContext(Map.of(ToolContextValues.USER_ID, 123));

        assertThat(ToolContextValues.userId(toolContext)).isEmpty();
    }

    @Test
    void userId_whenUserIdIsBlank_returnsEmpty() {
        ToolContext toolContext = new ToolContext(Map.of(ToolContextValues.USER_ID, "   "));

        assertThat(ToolContextValues.userId(toolContext)).isEmpty();
    }

    @Test
    void userId_whenUserIdPresent_returnsValue() {
        ToolContext toolContext = new ToolContext(Map.of(ToolContextValues.USER_ID, "anon:sess-abc"));

        assertThat(ToolContextValues.userId(toolContext)).contains("anon:sess-abc");
    }
}
