package org.learnings.ai.shoppingassistant.tools;

import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;
import java.util.Optional;

// TODO missing tests
/**
 * Keys and accessors for the per-request values that singleton {@code @Tool} beans need.
 *
 * <p>Spring AI's {@link ToolContext} is the supported way to pass request state into a tool without exposing it as a
 * model-visible argument. It travels with the request object, so unlike a {@code ThreadLocal} it survives the thread
 * hops that streaming and tool execution introduce.
 */
public final class ToolContextValues {

    public static final String USER_ID = "userId";

    private ToolContextValues() {
    }

    public static Map<String, Object> forUser(String userId) {
        return Map.of(USER_ID, userId);
    }

    public static Optional<String> userId(ToolContext toolContext) {
        if (toolContext == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(toolContext.getContext().get(USER_ID))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(id -> !id.isBlank());
    }
}
