package org.learnings.ai.shoppingassistant.infrastructure.repositories;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public class RedisChatMemoryRepositoryObservationDecorator implements ChatMemoryRepository {

    private static final String OBSERVATION_NAME = "chat_memory";

    private final ChatMemoryRepository delegate;
    private final ObservationRegistry registry;

    public RedisChatMemoryRepositoryObservationDecorator(ChatMemoryRepository delegate, ObservationRegistry registry) {
        this.delegate = delegate;
        this.registry = registry;
    }

    @Override
    public @NonNull List<Message> findByConversationId(@NonNull String id) {
        return observation("read")
                .observe(() -> delegate.findByConversationId(id));
    }

    @Override
    public void saveAll(@NonNull String id, @NonNull List<Message> messages) {
        observation("write")
                .observe(() -> delegate.saveAll(id, messages));
    }

    @Override
    public void deleteByConversationId(@NonNull String id) {
        observation("delete")
                .observe(() -> delegate.deleteByConversationId(id));
    }

    @Override
    public @NonNull List<String> findConversationIds() {
        return observation("list")
                .observe(delegate::findConversationIds);
    }

    private Observation observation(String operation) {
        return Observation.createNotStarted(OBSERVATION_NAME, registry)
                .lowCardinalityKeyValue("operation", operation);
    }
}
