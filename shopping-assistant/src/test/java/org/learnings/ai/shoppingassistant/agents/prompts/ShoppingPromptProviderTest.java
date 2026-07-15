package org.learnings.ai.shoppingassistant.agents.prompts;

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
class ShoppingPromptProviderTest {

    @Mock
    private ProductService productService;
    @Mock
    private UserMemoryService userMemoryService;

    private ShoppingPromptProvider shoppingPromptProvider;

    @BeforeEach
    void setup() {
        shoppingPromptProvider = new ShoppingPromptProvider(userMemoryService, new DefaultResourceLoader(), productService);
    }

    @Test
    void buildPrompt_whenCorrectParams_keepsStaticTemplateContent() {
        when(productService.getAllCategories()).thenReturn(List.of("categ1", "category 2"));
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = shoppingPromptProvider.buildPrompt("some user message");

        assertThat(prompt.getSystemMessage().getText())
                .startsWith("You are a helpful shopping assistant for")
                .contains("Today's date: ")
                .contains("Current language: ")
                .contains("Your responsibilities:")
                .contains("- Help users discover products and compare options.")
                .contains("- Answer product-related questions clearly and concisely.")
                .contains("- Use available tools when needed.")
                .contains("- If information is missing or uncertain, say so explicitly.")
                .contains("Product guidance:")
                .contains("- Available product categories: ");
        verifyNoMoreInteractions(productService, userMemoryService);
    }

    @Test
    void buildPrompt_whenCorrectParams_populatesTemplateVariables() {
        when(productService.getAllCategories()).thenReturn(List.of("categ1", "category 2"));
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = shoppingPromptProvider.buildPrompt("some user message");

        assertThat(prompt.getSystemMessage().getText())
                .contains("Current language: English")
                .contains("shopping assistant for Awesome Store")
                .contains("Available product categories: categ1,category 2")
                // leave no unresolved placeholders
                .doesNotContain("{today}")
                .doesNotContain("{language}")
                .doesNotContain("{storeName}")
                .doesNotContain("{categories}");
        verifyNoMoreInteractions(productService, userMemoryService);
    }

    @Test
    void buildPrompt_whenCategoriesEmpty_returnsEmptyCategory() {
        when(productService.getAllCategories()).thenReturn(List.of());
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = shoppingPromptProvider.buildPrompt("some user message");

        assertThat(prompt.getSystemMessage().getText())
                .contains("Available product categories: ");
        verifyNoMoreInteractions(productService, userMemoryService);
    }

    @Test
    void buildPrompt_whenCategoriesMissing_throws() {
        when(productService.getAllCategories()).thenReturn(null);

        assertThatThrownBy(() -> shoppingPromptProvider.buildPrompt("some user message"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(null);

        verifyNoMoreInteractions(productService, userMemoryService);
    }

    @Test
    void buildPrompt_whenCategoriesCallThrows_throws() {
        doThrow(new RuntimeException("timeout")).when(productService).getAllCategories();

        assertThatThrownBy(() -> shoppingPromptProvider.buildPrompt("some user message"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("timeout");

        verifyNoMoreInteractions(productService, userMemoryService);
    }

    @Test
    void buildPrompt_whenUserMessage_addsIt() {
        when(productService.getAllCategories()).thenReturn(List.of("categ1", "category 2"));
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = shoppingPromptProvider.buildPrompt("do you have red shoes?");

        assertThat(prompt.getUserMessage().getText()).isEqualTo("do you have red shoes?");
        verifyNoMoreInteractions(productService, userMemoryService);
    }

    @Test
    void buildPrompt_whenUserProfileExists_injectsUserContextMessage() {
        when(productService.getAllCategories()).thenReturn(List.of());
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.of("currency=EUR, size=M"));

        Prompt prompt = shoppingPromptProvider.buildPrompt("some message");

        String allSystemText = prompt.getInstructions().stream()
                .map(Content::getText)
                .collect(java.util.stream.Collectors.joining("\n"));
        assertThat(allSystemText).contains("Known information about the user: currency=EUR, size=M");
        verifyNoMoreInteractions(productService, userMemoryService);
    }

    @Test
    void buildPrompt_whenNoUserProfile_doesNotInjectUserContextMessage() {
        when(productService.getAllCategories()).thenReturn(List.of());
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = shoppingPromptProvider.buildPrompt("some message");

        String allSystemText = prompt.getInstructions().stream()
                .map(Content::getText)
                .collect(java.util.stream.Collectors.joining("\n"));
        assertThat(allSystemText).doesNotContain("Known information about the user");
        verifyNoMoreInteractions(productService, userMemoryService);
    }
}
