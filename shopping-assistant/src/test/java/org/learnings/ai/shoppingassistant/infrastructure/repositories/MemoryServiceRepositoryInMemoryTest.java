package org.learnings.ai.shoppingassistant.infrastructure.repositories;

import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.services.dtos.Message;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryServiceRepositoryInMemoryTest {

    private final MemoryServiceRepositoryInMemory repository = new MemoryServiceRepositoryInMemory();

    @Test
    void getConversation_whenNoMessages_returnsEmpty() {
        assertThat(repository.getConversation("missing")).isEmpty();
    }

    @Test
    void addMessageToConversation_whenNewConversation_storesMessage() {
        Message message = new Message("q", "a", (short) 0);

        repository.addMessageToConversation("c1", message);

        assertThat(repository.getConversation("c1")).containsExactly(message);
    }

    @Test
    void addMessageToConversation_whenExistingConversation_keepsMessagesOrderedByOrder() {
        Message first = new Message("q1", "a1", (short) 0);
        Message second = new Message("q2", "a2", (short) 1);

        repository.addMessageToConversation("c1", second);
        repository.addMessageToConversation("c1", first);

        assertThat(repository.getConversation("c1")).containsExactly(first, second);
    }
}
