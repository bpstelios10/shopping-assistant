package org.learnings.ai.shoppingassistant.agents.prompts;

import org.learnings.ai.shoppingassistant.services.memory.UserMemoryService;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.time.LocalDate.now;

@Component
public class SupportPromptProvider extends AbstractPromptProvider implements PromptProvider {

    private final PromptTemplate supportAssistantTemplate;

    public SupportPromptProvider(UserMemoryService userMemoryService, ResourceLoader resourceLoader) {
        super(userMemoryService);
        this.supportAssistantTemplate = new PromptTemplate(
                resourceLoader.getResource("classpath:prompts/support-system.st"));
    }

    @Override
    public Prompt buildPrompt(String userMessage) {
        String systemText = supportAssistantTemplate.render(
                Map.of(
                        "storeName", "Awesome Store",
                        "today", now(),
                        "language", "English"
                )
        );

        // low temperature - stick close to the docs. high tokens to return policies.
        // might need some space to explain or troubleshot steps
        ChatOptions supportChatOptions = ChatOptions.builder()
                .temperature(0.1)
                .maxTokens(800)
                .combineWith(OpenAiChatOptions.builder().extraBody(Map.of("think", false)))
                .build();
        PromptDecorator promptDecorator = PromptDecorator.builder()
                .withChatOptions(supportChatOptions)
                .withSystemMessage(systemText);

        getUserPreferences().ifPresent(promptDecorator::withSystemMessage);

        return promptDecorator
                .withUserMessage(userMessage)
                .build();
    }
}
