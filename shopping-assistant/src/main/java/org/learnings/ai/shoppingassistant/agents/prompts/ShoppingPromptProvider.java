package org.learnings.ai.shoppingassistant.agents.prompts;

import org.apache.logging.log4j.util.Strings;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryService;
import org.learnings.ai.shoppingassistant.services.products.ProductService;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.Map;

import static java.time.LocalDate.now;

// TODO i need to externalize this later. prompts often change, so it is more flexible to be
//  in a versioned prompt-management system (editable, A/B-tested, and rolled out without redeploying the service)
@Service
public class ShoppingPromptProvider implements PromptProvider {

    private final PromptTemplate shoppingAssistantTemplate;
    private final ProductService productService;
    private final UserMemoryService userMemoryService;

    public ShoppingPromptProvider(ResourceLoader resourceLoader, ProductService productService,
                                  UserMemoryService userMemoryService) {
        shoppingAssistantTemplate = new PromptTemplate(
                resourceLoader.getResource("classpath:prompts/shopping-system.st"));
        this.productService = productService;
        this.userMemoryService = userMemoryService;
    }

    @Override
    public Prompt buildPrompt(String userMessage) {
        String systemText = shoppingAssistantTemplate.render(
                Map.of(
                        "storeName", "Awesome Store",
                        "today", now(),
                        "language", "English",
                        "categories", Strings.join(productService.getAllCategories(), ',')
                )
        );

        // TODO: attach ChatOptions (e.g. temperature, model overrides) via
        //  .chatOptions(...), ideally sourced from @ConfigurationProperties, as prompting grows.
        return PromptDecorator.builder()
                .withSystemMessage(systemText)
                .withUserPreferences(userMemoryService)
                .withUserMessage(userMessage)
                .build();
    }
}
