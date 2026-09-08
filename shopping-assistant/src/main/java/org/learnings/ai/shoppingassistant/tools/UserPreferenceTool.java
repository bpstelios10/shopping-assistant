package org.learnings.ai.shoppingassistant.tools;

import lombok.extern.slf4j.Slf4j;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class UserPreferenceTool implements ShoppingAgentTool, OrderAgentTool {

    private final UserMemoryService userMemoryService;

    public UserPreferenceTool(UserMemoryService userMemoryService) {
        this.userMemoryService = userMemoryService;
    }

    /**
     * The {@link ToolContext} parameter is supplied by Spring AI and is not part of the schema advertised to the
     * model, so the caller identity can never be spoofed through a tool argument.
     */
    @Tool(description = "Persist a durable, long-term user preference (e.g. currency, size, "
            + "preferred brand, language, shipping address, price range). Call this ONLY when the "
            + "shopper states a lasting preference about themselves, not for one-off requests.")
    public String saveUserPreference(
            @ToolParam(description = "The preference key, e.g. 'currency', 'size', 'brand', 'language'.")
            String key,
            @ToolParam(description = "The preference value, e.g. 'EUR', 'M', 'Nike'.")
            String value,
            ToolContext toolContext) {
        Optional<String> userId = ToolContextValues.userId(toolContext);
        if (userId.isEmpty()) {
            log.warn("saveUserPreference called with no user in the tool context; skipping");
            return "Could not save preference: no user context.";
        }

        userMemoryService.updateProfile(userId.get(), Map.of(key, value));
        log.debug("saved preference for [{}]: {}={}", userId.get(), key, value);
        return "Saved preference " + key + "=" + value;
    }
}
