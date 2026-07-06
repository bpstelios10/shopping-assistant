package org.learnings.ai.shoppingassistant.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.services.products.ProductService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptServiceImplTest {

    @Mock
    private ProductService productService;

    private PromptServiceImpl promptService;

    @BeforeEach
    void setup() {
        promptService = new PromptServiceImpl(new DefaultResourceLoader(), productService);
    }

    @Test
    void buildShoppingAssistantPrompt_whenCorrectParams_keepsStaticTemplateContent() {
        when(productService.getAllCategories()).thenReturn(List.of("categ1", "category 2"));

        Prompt prompt = promptService.buildShoppingAssistantPrompt("some user message");

        assertThat(prompt.getSystemMessage().getText())
                .startsWith("You are a helpful shopping assistant.")
                .contains("Your responsibilities:")
                .contains("- Always be polite.")
                .contains("- Help users find products.")
                .contains("- Answer product questions.")
                .contains("- Be concise.")
                .contains("- If you don't know something, say so.");
        verifyNoMoreInteractions(productService);
    }

    @Test
    void buildShoppingAssistantPrompt_whenCorrectParams_populatesTemplateVariables() {
        when(productService.getAllCategories()).thenReturn(List.of("categ1", "category 2"));

        Prompt prompt = promptService.buildShoppingAssistantPrompt("some user message");

        assertThat(prompt.getSystemMessage().getText())
                .contains("Today's date: ")
                .contains("Current language: English")
                .contains("Store Name: Awesome Store")
                .contains("Available product categories: categ1,category 2");
        verifyNoMoreInteractions(productService);
    }

    @Test
    void buildShoppingAssistantPrompt_whenCorrectParams_leavesNoUnresolvedPlaceholders() {
        when(productService.getAllCategories()).thenReturn(List.of("categ1", "category 2"));

        Prompt prompt = promptService.buildShoppingAssistantPrompt("some user message");

        assertThat(prompt.getSystemMessage().getText())
                .doesNotContain("{today}")
                .doesNotContain("{language}")
                .doesNotContain("{storeName}")
                .doesNotContain("{categories}");
        verifyNoMoreInteractions(productService);
    }

    @Test
    void buildShoppingAssistantPrompt_whenCategoriesEmpty_returnsEmptyCategory() {
        when(productService.getAllCategories()).thenReturn(List.of());

        Prompt prompt = promptService.buildShoppingAssistantPrompt("some user message");

        assertThat(prompt.getSystemMessage().getText())
                .contains("Available product categories: ");
        verifyNoMoreInteractions(productService);
    }

    @Test
    void buildShoppingAssistantPrompt_whenCategoriesMissing_throws() {
        when(productService.getAllCategories()).thenReturn(null);

        assertThatThrownBy(() -> promptService.buildShoppingAssistantPrompt("some user message"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(null);

        verifyNoMoreInteractions(productService);
    }

    @Test
    void buildShoppingAssistantPrompt_whenCategoriesCallThrows_throws() {
        doThrow(new RuntimeException("timeout")).when(productService).getAllCategories();

        assertThatThrownBy(() -> promptService.buildShoppingAssistantPrompt("some user message"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("timeout");

        verifyNoMoreInteractions(productService);
    }

    @Test
    void buildShoppingAssistantPrompt_addsUserMessage() {
        Prompt prompt = promptService.buildShoppingAssistantPrompt("do you have red shoes?");

        assertThat(prompt.getUserMessage().getText()).isEqualTo("do you have red shoes?");
    }
}
