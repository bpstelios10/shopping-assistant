package org.learnings.ai.shoppingassistant.tools;

import lombok.extern.slf4j.Slf4j;
import org.learnings.ai.shoppingassistant.services.memory.CurrentUser;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class UserPreferenceTool implements AgentTool {

    private final UserMemoryService userMemoryService;

    public UserPreferenceTool(UserMemoryService userMemoryService) {
        this.userMemoryService = userMemoryService;
    }

    @Tool(description = "Persist a durable, long-term user preference (e.g. currency, size, "
            + "preferred brand, language, shipping address, price range). Call this ONLY when the "
            + "shopper states a lasting preference about themselves, not for one-off requests.")
    public String saveUserPreference(
            @ToolParam(description = "The preference key, e.g. 'currency', 'size', 'brand', 'language'.")
            String key,
            @ToolParam(description = "The preference value, e.g. 'EUR', 'M', 'Nike'.")
            String value) {

        String userId = CurrentUser.get();
        if (userId == null) {
            log.warn("saveUserPreference called with no current user; skipping");
            return "Could not save preference: no user context.";
        }
        userMemoryService.updateProfile(userId, Map.of(key, value));
        log.debug("saved preference for [{}]: {}={}", userId, key, value);
        return "Saved preference " + key + "=" + value;
    }
}
