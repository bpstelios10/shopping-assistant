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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPromptProviderTest {

    @Mock
    private UserMemoryService userMemoryService;

    private OrderPromptProvider orderPromptProvider;

    @BeforeEach
    void setup() {
        orderPromptProvider = new OrderPromptProvider(userMemoryService, new DefaultResourceLoader());
    }

    @Test
    void buildPrompt_whenCorrectParams_keepsStaticTemplateContent() {
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = orderPromptProvider.buildPrompt("some user message");

        assertThat(prompt.getSystemMessage().getText())
                .startsWith("You are the Order Agent for")
                .contains("Today's date is ")
                .contains("Your responsibility is to help customers manage their orders")
                .contains("You can assist with:")
                .contains("* Placing new orders")
                .contains("* Checking order status")
                .contains("* Cancelling orders")
                .contains("* Viewing order history")
                .contains("Guidelines:")
                .contains("* Be friendly, concise and professional.")
                .contains("* Use the available tools whenever order information is required.")
                .contains("* Never invent order details or statuses.")
                .contains("* If you cannot retrieve an order, explain the situation and ask the user for the required information.")
                .contains("* Do not answer questions about products, recommendations or store policies unless they are directly related to an order. Those requests should be handled by the appropriate agent.")
                .contains("* Respond in ");
        verifyNoMoreInteractions(userMemoryService);
    }

    @Test
    void buildPrompt_whenCorrectParams_populatesTemplateVariables() {
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = orderPromptProvider.buildPrompt("some user message");

        assertThat(prompt.getSystemMessage().getText())
                .contains("You are the Order Agent for Awesome Store")
                .contains("Respond in English")
                // leave no unresolved placeholders
                .doesNotContain("{today}")
                .doesNotContain("{language}")
                .doesNotContain("{storeName}")
                .doesNotContain("{categories}");
        verifyNoMoreInteractions(userMemoryService);
    }

    @Test
    void buildPrompt_whenUserMessage_addsIt() {
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = orderPromptProvider.buildPrompt("do you have red shoes?");

        assertThat(prompt.getUserMessage().getText()).isEqualTo("do you have red shoes?");
        verifyNoMoreInteractions(userMemoryService);
    }

    @Test
    void buildPrompt_whenUserProfileExists_injectsUserContextMessage() {
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.of("currency=EUR, size=M"));

        Prompt prompt = orderPromptProvider.buildPrompt("some message");

        String allSystemText = prompt.getInstructions().stream()
                .map(Content::getText)
                .collect(java.util.stream.Collectors.joining("\n"));
        assertThat(allSystemText).contains("Known information about the user: currency=EUR, size=M");
        verifyNoMoreInteractions(userMemoryService);
    }

    @Test
    void buildPrompt_whenNoUserProfile_doesNotInjectUserContextMessage() {
        when(userMemoryService.getProfileSummary(any())).thenReturn(Optional.empty());

        Prompt prompt = orderPromptProvider.buildPrompt("some message");

        String allSystemText = prompt.getInstructions().stream()
                .map(Content::getText)
                .collect(java.util.stream.Collectors.joining("\n"));
        assertThat(allSystemText).doesNotContain("Known information about the user");
        verifyNoMoreInteractions(userMemoryService);
    }
}
