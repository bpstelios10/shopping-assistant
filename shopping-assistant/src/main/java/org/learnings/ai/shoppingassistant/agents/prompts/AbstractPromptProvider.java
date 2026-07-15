package org.learnings.ai.shoppingassistant.agents.prompts;

import org.learnings.ai.shoppingassistant.services.memory.CurrentUser;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryService;

import java.util.Optional;

abstract class AbstractPromptProvider implements PromptProvider {

    private final UserMemoryService userMemoryService;

    protected AbstractPromptProvider(UserMemoryService userMemoryService) {
        this.userMemoryService = userMemoryService;
    }

    protected Optional<String> getUserPreferences() {
        return userMemoryService.getProfileSummary(CurrentUser.get())
                .map(s -> "Known information about the user: " + s);
    }
}
