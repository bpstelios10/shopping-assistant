package org.learnings.ai.shoppingassistant.services;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.Map;

import static java.time.LocalDate.now;

@Service
public class PromptServiceImpl implements PromptService {

    private final PromptTemplate shoppingAssistantTemplate;

    public PromptServiceImpl() {
        shoppingAssistantTemplate = new PromptTemplate(new ClassPathResource("prompts/shopping-assistant.st"));
    }

    @Override
    public String shoppingAssistantPrompt() {
        return shoppingAssistantTemplate.render(
                Map.of(
                        "today", now(),
                        "language", "English",
                        "storeName", "Awesome Store"
                )
        );
    }
}
