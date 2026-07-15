package org.learnings.ai.shoppingassistant.agents.prompts;

import org.learnings.ai.shoppingassistant.services.memory.UserMemoryService;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
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

        // TODO: attach ChatOptions (e.g. temperature, model overrides) via
        //  .chatOptions(...), ideally sourced from @ConfigurationProperties, as prompting grows.
        PromptDecorator promptDecorator = PromptDecorator.builder()
                .withSystemMessage(systemText);

        getUserPreferences().ifPresent(promptDecorator::withSystemMessage);

        return promptDecorator
                .withUserMessage(userMessage)
                .build();
    }
}
