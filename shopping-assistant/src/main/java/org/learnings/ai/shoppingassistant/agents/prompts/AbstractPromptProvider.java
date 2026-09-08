package org.learnings.ai.shoppingassistant.agents.prompts;

import org.learnings.ai.shoppingassistant.services.memory.UserMemoryService;

import java.util.Optional;

abstract class AbstractPromptProvider implements PromptProvider {

    private final UserMemoryService userMemoryService;

    protected AbstractPromptProvider(UserMemoryService userMemoryService) {
        this.userMemoryService = userMemoryService;
    }

    protected Optional<String> getUserPreferences(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        return userMemoryService.getProfileSummary(userId)
                .map(s -> "Known information about the user: " + s);
    }
}
