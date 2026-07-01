package org.learnings.ai.shoppingassistant.services;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class PromptServiceImplTest {

    private final PromptServiceImpl promptService = new PromptServiceImpl(new DefaultResourceLoader());

    @Test
    void buildShoppingAssistantPrompt_populatesTemplateVariables() {
        Prompt prompt = promptService.buildShoppingAssistantPrompt("some user message");

        assertThat(prompt.getSystemMessage().getText())
                .contains("Today's date: ")
                .contains("Current language: English")
                .contains("Store Name: Awesome Store");
    }

    @Test
    void buildShoppingAssistantPrompt_keepsStaticTemplateContent() {
        Prompt prompt = promptService.buildShoppingAssistantPrompt("some user message");

        assertThat(prompt.getSystemMessage().getText())
                .startsWith("You are a helpful shopping assistant.")
                .contains("Your responsibilities:")
                .contains("- Always be polite.")
                .contains("- Help users find products.")
                .contains("- Answer product questions.")
                .contains("- Be concise.")
                .contains("- If you don't know something, say so.");
    }

    @Test
    void buildShoppingAssistantPrompt_leavesNoUnresolvedPlaceholders() {
        Prompt prompt = promptService.buildShoppingAssistantPrompt("some user message");

        assertThat(prompt.getSystemMessage().getText())
                .doesNotContain("{today}")
                .doesNotContain("{language}")
                .doesNotContain("{storeName}");
    }

    @Test
    void buildShoppingAssistantPrompt_addsUserMessage() {
        Prompt prompt = promptService.buildShoppingAssistantPrompt("do you have red shoes?");

        assertThat(prompt.getUserMessage().getText()).isEqualTo("do you have red shoes?");
    }
}
