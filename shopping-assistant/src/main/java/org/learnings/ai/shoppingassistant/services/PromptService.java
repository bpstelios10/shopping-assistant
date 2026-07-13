package org.learnings.ai.shoppingassistant.services;

import org.springframework.ai.chat.prompt.Prompt;

public interface PromptService {

    Prompt buildShoppingAssistantPrompt(String userMessage, String userId);
}
