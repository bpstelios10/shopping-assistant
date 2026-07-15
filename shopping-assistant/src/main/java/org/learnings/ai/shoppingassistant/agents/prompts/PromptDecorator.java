package org.learnings.ai.shoppingassistant.agents.prompts;

import org.learnings.ai.shoppingassistant.services.memory.CurrentUser;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

public final class PromptDecorator {

    private final Prompt.Builder promptBuilder;
    private final List<Message> messages;

    private PromptDecorator(Prompt.Builder promptBuilder) {
        this.promptBuilder = promptBuilder;
        this.messages = new ArrayList<>();
        ;
    }

    public static PromptDecorator builder() {
        return new PromptDecorator(Prompt.builder());
    }

    public PromptDecorator with(Message message) {
        this.messages.add(message);
        return this;
    }

    public PromptDecorator withSystemMessage(String text) {
        return with(SystemMessage.builder().text(text).build());
    }

    public PromptDecorator withUserMessage(String text) {
        return with(UserMessage.builder().text(text).build());
    }

    public PromptDecorator withUserPreferences(UserMemoryService userMemoryService) {
        userMemoryService.getProfileSummary(CurrentUser.get())
                .ifPresent(summary -> with(SystemMessage.builder()
                        .text("Known information about the user: " + summary)
                        .build()));
        return this;
    }

    public Prompt build() {
        return promptBuilder.messages(messages).build();
    }
}
