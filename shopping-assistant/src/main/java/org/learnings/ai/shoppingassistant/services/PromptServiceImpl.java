package org.learnings.ai.shoppingassistant.services;

import org.apache.logging.log4j.util.Strings;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryService;
import org.learnings.ai.shoppingassistant.services.products.ProductService;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.time.LocalDate.now;

// TODO i need to externalize this later. prompts often change, so it is more flexible to be
//  in a versioned prompt-management system (editable, A/B-tested, and rolled out without redeploying the service)
@Service
public class PromptServiceImpl implements PromptService {

    private final PromptTemplate shoppingAssistantTemplate;
    private final ProductService productService;
    private final UserMemoryService userMemoryService;

    public PromptServiceImpl(ResourceLoader resourceLoader, ProductService productService,
                             UserMemoryService userMemoryService) {
        shoppingAssistantTemplate = new PromptTemplate(
                resourceLoader.getResource("classpath:prompts/shopping-system.st"));
        this.productService = productService;
        this.userMemoryService = userMemoryService;
    }

    @Override
    public Prompt buildShoppingAssistantPrompt(String userMessage, String userId) {
        String systemText = shoppingAssistantTemplate.render(
                Map.of(
                        "today", now(),
                        "language", "English",
                        "storeName", "Awesome Store",
                        "categories", Strings.join(productService.getAllCategories(), ',')
                )
        );

        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        messages.add(SystemMessage.builder().text(systemText).build());
        userMemoryService.getProfileSummary(userId)
                .ifPresent(summary -> messages.add(SystemMessage.builder()
                        .text("Known information about the user: " + summary)
                        .build()));
        messages.add(UserMessage.builder().text(userMessage).build());

        // TODO: attach ChatOptions (e.g. temperature, model overrides) via
        //  .chatOptions(...), ideally sourced from @ConfigurationProperties, as prompting grows.
        return Prompt.builder()
                .messages(messages)
                .build();
    }
}
