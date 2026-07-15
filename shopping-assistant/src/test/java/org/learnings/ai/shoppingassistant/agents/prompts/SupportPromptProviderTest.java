package org.learnings.ai.shoppingassistant.agents.prompts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Content;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportPromptProviderTest {

    @Mock
    private UserMemoryService userMemoryService;

    private SupportPromptProvider supportPromptProvider;

    @BeforeEach
    void setup() {
        supportPromptProvider = new SupportPromptProvider(userMemoryService, new DefaultResourceLoader());
    }

    @Test
    void buildPrompt_whenCorrectParams_keepsStaticTemplateContent() {
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = supportPromptProvider.buildPrompt("some user message");

        assertThat(prompt.getSystemMessage().getText())
                .startsWith("You are a helpful customer support assistant for")
                .contains("Today's date: ")
                .contains("Current language: ")
                .contains("Your responsibilities:")
                .contains("- Answer customer support questions (returns, refunds, shipping, policies, account/help topics).")
                .contains("- Prefer facts from retrieved support knowledge.")
                .contains("- If support knowledge is missing, say you do not have enough information.")
                .contains("- Be concise, polite, and practical.")
                .contains("Response style:")
                .contains("- Provide direct answers first.")
                .contains("- Add short next steps only when helpful.");
        verifyNoMoreInteractions(userMemoryService);
    }

    @Test
    void buildPrompt_whenCorrectParams_populatesTemplateVariables() {
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = supportPromptProvider.buildPrompt("some user message");

        assertThat(prompt.getSystemMessage().getText())
                .startsWith("You are a helpful customer support assistant for Awesome Store.")
                .contains("Current language: English")
                // leave no unresolved placeholders
                .doesNotContain("{today}")
                .doesNotContain("{language}")
                .doesNotContain("{storeName}");
        verifyNoMoreInteractions(userMemoryService);
    }

    @Test
    void buildPrompt_whenUserMessage_addsIt() {
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = supportPromptProvider.buildPrompt("do you have red shoes?");

        assertThat(prompt.getUserMessage().getText()).isEqualTo("do you have red shoes?");
        verifyNoMoreInteractions(userMemoryService);
    }

    @Test
    void buildPrompt_whenUserProfileExists_injectsUserContextMessage() {
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.of("currency=EUR, size=M"));

        Prompt prompt = supportPromptProvider.buildPrompt("some message");

        String allSystemText = prompt.getInstructions().stream()
                .map(Content::getText)
                .collect(java.util.stream.Collectors.joining("\n"));
        assertThat(allSystemText).contains("Known information about the user: currency=EUR, size=M");
        verifyNoMoreInteractions(userMemoryService);
    }

    @Test
    void buildPrompt_whenNoUserProfile_doesNotInjectUserContextMessage() {
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = supportPromptProvider.buildPrompt("some message");

        String allSystemText = prompt.getInstructions().stream()
                .map(Content::getText)
                .collect(java.util.stream.Collectors.joining("\n"));
        assertThat(allSystemText).doesNotContain("Known information about the user");
        verifyNoMoreInteractions(userMemoryService);
    }
}
