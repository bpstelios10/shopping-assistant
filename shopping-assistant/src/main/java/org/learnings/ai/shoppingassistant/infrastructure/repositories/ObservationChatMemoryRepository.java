package org.learnings.ai.shoppingassistant.infrastructure.repositories;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public class ObservationChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMemoryRepository delegate;
    private final ObservationRegistry registry;

    public ObservationChatMemoryRepository(ChatMemoryRepository delegate, ObservationRegistry registry) {
        this.delegate = delegate;
        this.registry = registry;
    }

    @Override
    public @NonNull List<Message> findByConversationId(@NonNull String id) {
        return Observation.createNotStarted("chat_memory", registry)
                .lowCardinalityKeyValue("operation", "read")
                .observe(() -> delegate.findByConversationId(id));
    }

    @Override
    public void saveAll(@NonNull String id, @NonNull List<Message> messages) {
        Observation.createNotStarted("chat_memory", registry)
                .lowCardinalityKeyValue("operation", "write")
                .observe(() -> delegate.saveAll(id, messages));
    }

    @Override
    public void deleteByConversationId(@NonNull String id) {
        Observation.createNotStarted("chat_memory", registry)
                .lowCardinalityKeyValue("operation", "delete")
                .observe(() -> delegate.deleteByConversationId(id));
    }

    @Override
    public @NonNull List<String> findConversationIds() {
        return Observation.createNotStarted("chat_memory", registry)
                .lowCardinalityKeyValue("operation", "list")
                .observe(delegate::findConversationIds);
    }
}
