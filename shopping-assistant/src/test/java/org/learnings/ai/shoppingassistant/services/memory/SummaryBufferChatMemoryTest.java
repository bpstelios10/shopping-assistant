package org.learnings.ai.shoppingassistant.services.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryBufferChatMemoryTest {

    private static final String CONVERSATION_ID = "conv-1";

    @Mock
    private ChatModel chatModel;

    private InMemoryRepository repository;
    private SummaryBufferChatMemory memory;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepository();
        memory = new SummaryBufferChatMemory(repository, chatModel, 10, 20);
    }

    @Test
    void constructor_whenBufferIsNotGreaterThanWindow_throws() {
        assertThatThrownBy(() -> new SummaryBufferChatMemory(repository, chatModel, 10, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bufferSize must be > windowSize");
    }

    @Test
    void constructor_whenWindowIsNotPositive_throws() {
        assertThatThrownBy(() -> new SummaryBufferChatMemory(repository, chatModel, 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("windowSize must be > 0");
    }

    @Test
    void get_whenNoSummary_returnsOnlyLastWindowMessages() {
        repository.saveAll(CONVERSATION_ID, messages(12));

        List<Message> result = memory.get(CONVERSATION_ID);

        assertThat(result).hasSize(10);
        assertThat(result.getFirst().getText()).isEqualTo("m-3");
        assertThat(result.getLast().getText()).isEqualTo("m-12");
    }

    @Test
    void get_whenSummaryExists_prependsSummaryBeforeWindow() {
        repository.saveAll(CONVERSATION_ID, List.of(
                new SystemMessage("Summary of earlier conversation:\nuser likes red shoes"),
                new UserMessage("m-1"),
                new UserMessage("m-2")
        ));

        List<Message> result = memory.get(CONVERSATION_ID);

        assertThat(result).hasSize(3);
        assertThat(result.getFirst()).isInstanceOf(SystemMessage.class);
        assertThat(result.getFirst().getText()).contains("user likes red shoes");
        assertThat(result.get(1).getText()).isEqualTo("m-1");
        assertThat(result.get(2).getText()).isEqualTo("m-2");
    }

    @Test
    void get_whenStoredSystemMessageHasNullText_ignoresIt() {
        Message systemMessage = mock(Message.class);
        when(systemMessage.getMessageType()).thenReturn(org.springframework.ai.chat.messages.MessageType.SYSTEM);
        when(systemMessage.getText()).thenReturn(null);
        repository.saveAll(CONVERSATION_ID, List.of(systemMessage, new UserMessage("m-1"), new UserMessage("m-2")));

        List<Message> result = memory.get(CONVERSATION_ID);

        assertThat(result).hasSize(3);
        assertThat(result.getFirst()).isSameAs(systemMessage);
        assertThat(result.get(1).getText()).isEqualTo("m-1");
        assertThat(result.get(2).getText()).isEqualTo("m-2");
    }

    @Test
    void get_whenStoredSystemMessageDoesNotStartWithSummaryPrefix_ignoresIt() {
        SystemMessage otherSystemMessage = new SystemMessage("not-a-summary");
        repository.saveAll(CONVERSATION_ID, List.of(otherSystemMessage, new UserMessage("m-1"), new UserMessage("m-2")));

        List<Message> result = memory.get(CONVERSATION_ID);

        assertThat(result).hasSize(3);
        assertThat(result.getFirst()).isSameAs(otherSystemMessage);
        assertThat(result.get(1).getText()).isEqualTo("m-1");
        assertThat(result.get(2).getText()).isEqualTo("m-2");
    }

    @Test
    void add_whenUnderBuffer_doesNotSummarize() {
        memory.add(CONVERSATION_ID, messages(5));

        List<Message> stored = repository.findByConversationId(CONVERSATION_ID);
        assertThat(stored).hasSize(5);
        verifyNoInteractions(chatModel);
    }

    @Test
    void add_whenBufferReached_compactsOldMessagesIntoSummary() {
        when(chatModel.call(any(Prompt.class))).thenReturn(summaryResponse("compact-summary"));
        repository.saveAll(CONVERSATION_ID, messages(19));

        memory.add(CONVERSATION_ID, List.of(new UserMessage("m-20")));

        List<Message> stored = repository.findByConversationId(CONVERSATION_ID);
        assertThat(stored).hasSize(11);
        assertThat(stored.getFirst()).isInstanceOf(SystemMessage.class);
        assertThat(stored.getFirst().getText())
                .startsWith("Summary of earlier conversation:\n")
                .contains("compact-summary");
        assertThat(stored.get(1).getText()).isEqualTo("m-11");
        assertThat(stored.getLast().getText()).isEqualTo("m-20");
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void add_whenExistingSummaryAndBufferReached_preservesExistingSummaryText() {
        when(chatModel.call(any(Prompt.class))).thenReturn(summaryResponse("updated-summary"));
        repository.saveAll(CONVERSATION_ID, List.of(new SystemMessage("Summary of earlier conversation:\nold-summary")));
        repository.saveAll(CONVERSATION_ID, append(repository.findByConversationId(CONVERSATION_ID), messages(20)));

        memory.add(CONVERSATION_ID, List.of(new UserMessage("m-21")));

        List<Message> stored = repository.findByConversationId(CONVERSATION_ID);
        assertThat(stored.getFirst()).isInstanceOf(SystemMessage.class);
        assertThat(stored.getFirst().getText()).contains("updated-summary");
        assertThat(stored).hasSize(11);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void add_whenMoreMessagesArrive_summarizesInBatchesNotEveryTurn() {
        when(chatModel.call(any(Prompt.class))).thenReturn(summaryResponse("batch-summary"));
        repository.saveAll(CONVERSATION_ID, messages(19));

        memory.add(CONVERSATION_ID, List.of(new UserMessage("m-20"))); // 1st compaction
        for (int i = 21; i <= 29; i++) {
            memory.add(CONVERSATION_ID, List.of(new UserMessage("m-" + i)));
        }
        memory.add(CONVERSATION_ID, List.of(new UserMessage("m-30"))); // 2nd compaction

        verify(chatModel, times(2)).call(any(Prompt.class));
        List<Message> stored = repository.findByConversationId(CONVERSATION_ID);
        assertThat(stored).hasSize(11);
        assertThat(stored.getFirst()).isInstanceOf(SystemMessage.class);
        assertThat(stored.get(1).getText()).isEqualTo("m-21");
        assertThat(stored.getLast().getText()).isEqualTo("m-30");
    }

    @Test
    void clear_whenCalled_deletesConversation() {
        repository.saveAll(CONVERSATION_ID, messages(3));

        memory.clear(CONVERSATION_ID);

        assertThat(repository.findByConversationId(CONVERSATION_ID)).isEmpty();
    }

    private static ChatResponse summaryResponse(String summaryText) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(summaryText))));
    }

    private static List<Message> messages(int count) {
        List<Message> messages = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            messages.add(new UserMessage("m-" + i));
        }
        return messages;
    }

    private static List<Message> append(List<Message> first, List<Message> second) {
        List<Message> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }

    private static class InMemoryRepository implements ChatMemoryRepository {

        private final Map<String, List<Message>> store = new HashMap<>();

        @Override
        public @NonNull List<String> findConversationIds() {
            return new ArrayList<>(store.keySet());
        }

        @Override
        public @NonNull List<Message> findByConversationId(@NonNull String conversationId) {
            return new ArrayList<>(store.getOrDefault(conversationId, List.of()));
        }

        @Override
        public void saveAll(@NonNull String conversationId, @NonNull List<Message> messages) {
            store.put(conversationId, new ArrayList<>(messages));
        }

        @Override
        public void deleteByConversationId(@NonNull String conversationId) {
            store.remove(conversationId);
        }
    }
}
