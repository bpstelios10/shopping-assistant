package org.learnings.ai.shoppingassistant.services;

import org.learnings.ai.shoppingassistant.services.dtos.Message;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.NavigableSet;

public interface PromptService {
    Prompt buildShoppingAssistantPrompt(String userMessage, NavigableSet<Message> conversationHistory);
}
