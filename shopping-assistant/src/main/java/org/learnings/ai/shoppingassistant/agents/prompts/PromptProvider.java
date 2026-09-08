package org.learnings.ai.shoppingassistant.agents.prompts;

import org.springframework.ai.chat.prompt.Prompt;

public interface PromptProvider {

    /**
     * @param userMessage the shopper's message for this turn
     * @param userId      the caller, used to inject stored preferences; may be {@code null}
     */
    Prompt buildPrompt(String userMessage, String userId);
}
