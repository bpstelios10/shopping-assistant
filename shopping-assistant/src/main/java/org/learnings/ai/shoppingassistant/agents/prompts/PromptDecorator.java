package org.learnings.ai.shoppingassistant.agents.prompts;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

public final class PromptDecorator {

    private static final String LATEST_TURN_GUARD = """
            Answer ONLY the user's latest message above.
            The earlier user/assistant turns are prior conversation history, provided for context only.
            Do NOT re-answer or continue previous questions unless the latest message explicitly asks you to.""";

    private final Prompt.Builder promptBuilder;
    private final List<Message> messages;
    private ChatOptions chatOptions;

    private PromptDecorator(Prompt.Builder promptBuilder) {
        this.promptBuilder = promptBuilder;
        this.messages = new ArrayList<>();
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

    public PromptDecorator withChatOptions(ChatOptions chatOptions) {
        this.chatOptions = chatOptions;
        return this;
    }

    public Prompt build() {
        List<Message> finalMessages = new ArrayList<>(messages);
        finalMessages.add(SystemMessage.builder().text(LATEST_TURN_GUARD).build());
        if (this.chatOptions != null) {
            promptBuilder.chatOptions(this.chatOptions);
        }

        return promptBuilder
                .messages(finalMessages)
                .build();
    }
}
