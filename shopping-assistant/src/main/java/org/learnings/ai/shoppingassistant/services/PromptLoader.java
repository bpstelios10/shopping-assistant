package org.learnings.ai.shoppingassistant.services;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Loads prompt templates from {@code classpath:/prompts/} by file name and
 * renders them with the supplied variables. Keeps all Resource/PromptTemplate
 * plumbing out of the services.
 */
@Component
public class PromptLoader {

    private static final String PROMPTS_LOCATION = "classpath:/prompts/";

    private final ResourceLoader resourceLoader;

    public PromptLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String render(String templateName, Map<String, Object> variables) {
        Resource resource = resourceLoader.getResource(PROMPTS_LOCATION + templateName);
        return new PromptTemplate(resource).render(variables);
    }
}

