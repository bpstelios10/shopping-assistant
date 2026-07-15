package org.learnings.ai.shoppingassistant.agents.prompts;

import org.springframework.ai.chat.prompt.Prompt;

public interface PromptProvider {

    Prompt buildPrompt(String userMessage);
}
