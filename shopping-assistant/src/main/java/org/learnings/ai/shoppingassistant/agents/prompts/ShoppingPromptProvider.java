package org.learnings.ai.shoppingassistant.agents.prompts;

import org.apache.logging.log4j.util.Strings;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryService;
import org.learnings.ai.shoppingassistant.services.products.ProductService;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.Map;

import static java.time.LocalDate.now;

// TODO i need to externalize this later. prompts often change, so it is more flexible to be
//  in a versioned prompt-management system (editable, A/B-tested, and rolled out without redeploying the service)
@Service
public class ShoppingPromptProvider extends AbstractPromptProvider implements PromptProvider {

    private final PromptTemplate shoppingAssistantTemplate;
    private final ProductService productService;

    public ShoppingPromptProvider(UserMemoryService userMemoryService, ResourceLoader resourceLoader,
                                  ProductService productService) {
        super(userMemoryService);
        shoppingAssistantTemplate = new PromptTemplate(
                resourceLoader.getResource("classpath:prompts/shopping-system.st"));
        this.productService = productService;
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

        // low temperature - stick close to tool. but with some more flexibility for interpreting product questions.
        // might need some space for long descriptions or return multiple products
        ChatOptions shoppingChatOptions = ChatOptions
                .builder()
                .temperature(0.2)
                .maxTokens(1000)
                .build();
        PromptDecorator promptDecorator = PromptDecorator
                .builder()
                .withChatOptions(shoppingChatOptions)
                .withSystemMessage(systemText);

        getUserPreferences().ifPresent(promptDecorator::withSystemMessage);

        return promptDecorator
                .withUserMessage(userMessage)
                .build();
    }
}
