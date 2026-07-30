package org.learnings.ai.shoppingassistant.agents.prompts;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptDecoratorTest {

    @Test
    void build_whenUserMessageProvided_appendsLatestTurnGuardSystemMessage() {
        Prompt prompt = PromptDecorator.builder()
                .withUserMessage("Hello")
                .build();

        List<Message> messages = prompt.getInstructions();

        assertThat(messages).hasSize(2);

        assertThat(messages.getFirst()).isInstanceOf(UserMessage.class);
        UserMessage first = (UserMessage) messages.getFirst();
        assertThat(first.getText()).isEqualTo("Hello");

        assertThat(messages.get(1)).isInstanceOf(SystemMessage.class);
        SystemMessage last = (SystemMessage) messages.get(1);
        assertThat(last.getText()).contains("Answer ONLY the user's latest message above.");
    }

    @Test
    void build_whenSystemAndUserMessagesProvided_preservesExistingMessages() {
        Prompt prompt = PromptDecorator.builder()
                .withSystemMessage("System instruction")
                .withUserMessage("Hello")
                .build();

        assertThat(prompt.getSystemMessages())
                .extracting(Message::getText)
                .containsExactly(
                        "System instruction",
                        """
                                Answer ONLY the user's latest message above.
                                The earlier user/assistant turns are prior conversation history, provided for context only.
                                Do NOT re-answer or continue previous questions unless the latest message explicitly asks you to."""
                );

        assertThat(prompt.getUserMessages())
                .extracting(Message::getText)
                .containsExactly("Hello");
    }

    @Test
    void with_whenMessageProvided_addsMessageToPrompt() {
        UserMessage message = UserMessage.builder()
                .text("Custom message")
                .build();

        Prompt prompt = PromptDecorator.builder()
                .with(message)
                .build();

        assertThat(prompt.getUserMessages())
                .contains(message);
    }

    @Test
    void withSystemMessage_whenTextProvided_addsSystemMessageToPrompt() {
        Prompt prompt = PromptDecorator.builder()
                .withSystemMessage("System message")
                .build();

        assertThat(prompt.getSystemMessages())
                .extracting(Message::getText)
                .contains("System message");
    }

    @Test
    void withUserMessage_whenTextProvided_addsUserMessageToPrompt() {
        Prompt prompt = PromptDecorator.builder()
                .withUserMessage("User message")
                .build();

        assertThat(prompt.getUserMessages())
                .extracting(Message::getText)
                .containsExactly("User message");
    }

    @Test
    void build_whenNoMessagesProvided_shouldOnlyContainGuardMessage() {
        Prompt prompt = PromptDecorator.builder()
                .build();

        assertThat(prompt.getUserMessages())
                .isEmpty();

        assertThat(prompt.getSystemMessages())
                .hasSize(1)
                .first()
                .extracting(Message::getText)
                .asString()
                .contains("Answer ONLY the user's latest message above.");
    }

    @Test
    void build_shouldIncludeChatOptionsWhenProvided() {
        ChatOptions options = ChatOptions.builder()
                .build();

        Prompt prompt = PromptDecorator.builder()
                .withChatOptions(options)
                .build();

        assertThat(prompt.getOptions()).isEqualTo(options);
    }

    @Test
    void build_shouldNotModifyPreviouslyBuiltPrompt() {
        PromptDecorator decorator = PromptDecorator.builder()
                .withUserMessage("First");

        Prompt firstBuild = decorator.build();

        decorator.withUserMessage("Second");

        Prompt secondBuild = decorator.build();

        assertThat(firstBuild.getUserMessages())
                .extracting(Message::getText)
                .containsExactly("First");

        assertThat(secondBuild.getUserMessages())
                .extracting(Message::getText)
                .containsExactly("First", "Second");
    }

    @Test
    void methods_shouldSupportFluentChaining() {
        PromptDecorator decorator = PromptDecorator.builder()
                .withSystemMessage("System")
                .withUserMessage("User")
                .withChatOptions(ChatOptions.builder().build());

        assertThat(decorator).isNotNull();
    }
}
