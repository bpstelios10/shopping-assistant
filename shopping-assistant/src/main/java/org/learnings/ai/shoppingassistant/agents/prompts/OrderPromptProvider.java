package org.learnings.ai.shoppingassistant.agents.prompts;

import org.learnings.ai.shoppingassistant.services.memory.UserMemoryService;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.Map;

import static java.time.LocalDate.now;

@Service
public class OrderPromptProvider extends AbstractPromptProvider implements PromptProvider {

    private final PromptTemplate orderAssistantTemplate;

    public OrderPromptProvider(UserMemoryService userMemoryService, ResourceLoader resourceLoader) {
        super(userMemoryService);
        orderAssistantTemplate = new PromptTemplate(
                resourceLoader.getResource("classpath:prompts/order-system.st"));
    }

    @Override
    public Prompt buildPrompt(String userMessage) {
        String systemText = orderAssistantTemplate.render(
                Map.of(
                        "storeName", "Awesome Store",
                        "today", now(),
                        "language", "English"
                )
        );

        // low temperature - stick close to tool. Extract parameters, call tool, report facts.
        // low tokens too. Short factual responses.
        ChatOptions orderChatOptions = ChatOptions
                .builder()
                .temperature(0.1)
                .maxTokens(400)
                .build();
        PromptDecorator promptDecorator = PromptDecorator
                .builder()
                .withChatOptions(orderChatOptions)
                .withSystemMessage(systemText);

        getUserPreferences().ifPresent(promptDecorator::withSystemMessage);

        return promptDecorator
                .withUserMessage(userMessage)
                .build();
    }
}
