package org.learnings.ai.shoppingassistant.services;

import org.junit.jupiter.api.Test;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;

class PromptServiceImplTest {

    private final PromptServiceImpl promptService = new PromptServiceImpl();

    @Test
    void shoppingAssistantPrompt_populatesTemplateVariables() {
        String prompt = promptService.shoppingAssistantPrompt();

        assertThat(prompt)
                .contains("Today's date: " + now())
                .contains("Current language: English")
                .contains("Store Name: Awesome Store");
    }

    @Test
    void shoppingAssistantPrompt_keepsStaticTemplateContent() {
        String prompt = promptService.shoppingAssistantPrompt();

        assertThat(prompt)
                .startsWith("You are a helpful shopping assistant.")
                .contains("Your responsibilities:")
                .contains("- Always be polite.")
                .contains("- Help users find products.")
                .contains("- Answer product questions.")
                .contains("- Be concise.")
                .contains("- If you don't know something, say so.");
    }

    @Test
    void shoppingAssistantPrompt_leavesNoUnresolvedPlaceholders() {
        String prompt = promptService.shoppingAssistantPrompt();

        assertThat(prompt)
                .doesNotContain("{today}")
                .doesNotContain("{language}")
                .doesNotContain("{storeName}");
    }
}
