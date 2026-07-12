package org.learnings.ai.shoppingassistant.services.memory;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Summary-buffer memory strategy:
 * - Keep latest N raw messages (windowSize) for precise follow-ups.
 * - Keep older context as one running summary (SystemMessage with SUMMARY_PREFIX).
 * - Compact only when raw messages reach bufferSize to avoid per-turn summarization.
 * <p>
 * Stored shape per conversation:
 * [optional summary message] + [latest raw window]
 * <p>
 * Rule: bufferSize must be > windowSize.
 */
@Slf4j
public class SummaryBufferChatMemory implements ChatMemory {

    private static final String SUMMARY_PREFIX = "Summary of earlier conversation:\n";

    private final ChatMemoryRepository repository;
    private final ChatModel chatModel;
    private final int windowSize;
    private final int bufferSize;

    public SummaryBufferChatMemory(ChatMemoryRepository repository, ChatModel chatModel, int windowSize, int bufferSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be > 0");
        }
        if (bufferSize <= windowSize) {
            throw new IllegalArgumentException("bufferSize must be > windowSize");
        }
        this.repository = repository;
        this.chatModel = chatModel;
        this.windowSize = windowSize;
        this.bufferSize = bufferSize;
    }

    @Override
    public void add(@NonNull String conversationId, @NonNull List<Message> messages) {
        List<Message> stored = new ArrayList<>(repository.findByConversationId(conversationId));
        stored.addAll(messages);

        SystemMessage existingSummary = extractSummary(stored);
        List<Message> raw = stripSummary(stored, existingSummary);

        // Compact in batches: fold only when raw messages hit the buffer threshold.
        if (raw.size() >= bufferSize) {
            // Keep the latest window raw; fold all older raw messages into the running summary.
            int foldCount = raw.size() - windowSize;
            List<Message> toFold = raw.subList(0, foldCount);
            List<Message> keep = new ArrayList<>(raw.subList(foldCount, raw.size()));

            SystemMessage newSummary = summarize(existingSummary, toFold);

            List<Message> compacted = new ArrayList<>();
            compacted.add(newSummary);
            compacted.addAll(keep);
            repository.saveAll(conversationId, compacted);
            log.debug("compacted conversation [{}]: folded {} messages", conversationId, foldCount);
        } else {
            repository.saveAll(conversationId, stored);
        }
    }

    @Override
    public @NonNull List<Message> get(@NonNull String conversationId) {
        List<Message> stored = repository.findByConversationId(conversationId);
        SystemMessage summary = extractSummary(stored);
        List<Message> raw = stripSummary(stored, summary);

        List<Message> window = raw.size() > windowSize ? raw.subList(raw.size() - windowSize, raw.size()) : raw;

        List<Message> result = new ArrayList<>();
        if (summary != null) {
            result.add(summary);
        }
        result.addAll(window);

        return result;
    }

    @Override
    public void clear(@NonNull String conversationId) {
        repository.deleteByConversationId(conversationId);
    }

    private SystemMessage extractSummary(List<Message> messages) {
        if (!messages.isEmpty()
                && messages.getFirst().getMessageType() == MessageType.SYSTEM
                && messages.getFirst().getText() != null
                && messages.getFirst().getText().startsWith(SUMMARY_PREFIX)) {
            return (SystemMessage) messages.getFirst();
        }

        return null;
    }

    private List<Message> stripSummary(List<Message> messages, SystemMessage summary) {
        return summary == null ? new ArrayList<>(messages) : new ArrayList<>(messages.subList(1, messages.size()));
    }

    private SystemMessage summarize(SystemMessage existingSummary, List<Message> toFold) {
        StringBuilder convo = new StringBuilder();
        if (existingSummary != null) {
            convo.append(existingSummary.getText()).append("\n\n");
        }
        toFold.forEach(m -> convo.append(m.getMessageType()).append(": ")
                .append(m.getText()).append("\n"));

        String instruction = """
                Update the running summary of this conversation.
                Preserve durable facts: user preferences, decisions, IDs, currency, sizes, \
                shipping details, and unresolved questions. Be concise. Output ONLY the summary.
                
                Conversation to summarize:
                %s
                """.formatted(convo);

        String summaryText =
                Objects.requireNonNull(chatModel.call(new Prompt(instruction)).getResult())
                        .getOutput().getText();

        return new SystemMessage(SUMMARY_PREFIX + summaryText);
    }
}
