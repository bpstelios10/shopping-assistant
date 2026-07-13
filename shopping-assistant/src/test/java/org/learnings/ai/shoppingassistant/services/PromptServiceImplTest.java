package org.learnings.ai.shoppingassistant.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryService;
import org.learnings.ai.shoppingassistant.services.products.ProductService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Content;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptServiceImplTest {

    @Mock
    private ProductService productService;
    @Mock
    private UserMemoryService userMemoryService;

    private PromptServiceImpl promptService;

    @BeforeEach
    void setup() {
        promptService = new PromptServiceImpl(new DefaultResourceLoader(), productService, userMemoryService);
    }

    @Test
    void buildShoppingAssistantPrompt_whenCorrectParams_keepsStaticTemplateContent() {
        when(productService.getAllCategories()).thenReturn(List.of("categ1", "category 2"));
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = promptService.buildShoppingAssistantPrompt("some user message", "user-1");

        assertThat(prompt.getSystemMessage().getText())
                .startsWith("You are a helpful shopping assistant.")
                .contains("Your responsibilities:")
                .contains("- Always be polite.")
                .contains("- Help users find products.")
                .contains("- Answer product questions.")
                .contains("- Be concise.")
                .contains("- If you don't know something, say so.");
        verifyNoMoreInteractions(productService, userMemoryService);
    }

    @Test
    void buildShoppingAssistantPrompt_whenCorrectParams_populatesTemplateVariables() {
        when(productService.getAllCategories()).thenReturn(List.of("categ1", "category 2"));
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = promptService.buildShoppingAssistantPrompt("some user message", "user-1");

        assertThat(prompt.getSystemMessage().getText())
                .contains("Today's date: ")
                .contains("Current language: English")
                .contains("Store Name: Awesome Store")
                .contains("Available product categories: categ1,category 2");
        verifyNoMoreInteractions(productService, userMemoryService);
    }

    @Test
    void buildShoppingAssistantPrompt_whenCorrectParams_leavesNoUnresolvedPlaceholders() {
        when(productService.getAllCategories()).thenReturn(List.of("categ1", "category 2"));
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = promptService.buildShoppingAssistantPrompt("some user message", "user-1");

        assertThat(prompt.getSystemMessage().getText())
                .doesNotContain("{today}")
                .doesNotContain("{language}")
                .doesNotContain("{storeName}")
                .doesNotContain("{categories}");
        verifyNoMoreInteractions(productService, userMemoryService);
    }

    @Test
    void buildShoppingAssistantPrompt_whenCategoriesEmpty_returnsEmptyCategory() {
        when(productService.getAllCategories()).thenReturn(List.of());
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = promptService.buildShoppingAssistantPrompt("some user message", "user-1");

        assertThat(prompt.getSystemMessage().getText())
                .contains("Available product categories: ");
        verifyNoMoreInteractions(productService, userMemoryService);
    }

    @Test
    void buildShoppingAssistantPrompt_whenCategoriesMissing_throws() {
        when(productService.getAllCategories()).thenReturn(null);

        assertThatThrownBy(() -> promptService.buildShoppingAssistantPrompt("some user message", "user-1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(null);

        verifyNoMoreInteractions(productService, userMemoryService);
    }

    @Test
    void buildShoppingAssistantPrompt_whenCategoriesCallThrows_throws() {
        doThrow(new RuntimeException("timeout")).when(productService).getAllCategories();

        assertThatThrownBy(() -> promptService.buildShoppingAssistantPrompt("some user message", "user-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("timeout");

        verifyNoMoreInteractions(productService, userMemoryService);
    }

    @Test
    void buildShoppingAssistantPrompt_addsUserMessage() {
        when(productService.getAllCategories()).thenReturn(List.of("categ1", "category 2"));
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = promptService.buildShoppingAssistantPrompt("do you have red shoes?", "user-1");

        assertThat(prompt.getUserMessage().getText()).isEqualTo("do you have red shoes?");
        verifyNoMoreInteractions(productService, userMemoryService);
    }

    @Test
    void buildShoppingAssistantPrompt_whenUserProfileExists_injectsUserContextMessage() {
        when(productService.getAllCategories()).thenReturn(List.of());
        when(userMemoryService.getProfileSummary("user-1")).thenReturn(Optional.of("currency=EUR, size=M"));

        Prompt prompt = promptService.buildShoppingAssistantPrompt("some message", "user-1");

        String allSystemText = prompt.getInstructions().stream()
                .map(Content::getText)
                .collect(java.util.stream.Collectors.joining("\n"));
        assertThat(allSystemText).contains("Known information about the user: currency=EUR, size=M");
        verifyNoMoreInteractions(productService, userMemoryService);
    }

    @Test
    void buildShoppingAssistantPrompt_whenNoUserProfile_doesNotInjectUserContextMessage() {
        when(productService.getAllCategories()).thenReturn(List.of());
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = promptService.buildShoppingAssistantPrompt("some message", "user-1");

        String allSystemText = prompt.getInstructions().stream()
                .map(Content::getText)
                .collect(java.util.stream.Collectors.joining("\n"));
        assertThat(allSystemText).doesNotContain("Known information about the user");
        verifyNoMoreInteractions(productService, userMemoryService);
    }
}
