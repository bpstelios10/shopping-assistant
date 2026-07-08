package org.learnings.ai.shoppingassistant.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.services.dtos.Message;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NavigableSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryServiceImplTest {

    private static final String CONVERSATION_ID = "some-conversation-id";

    @Mock
    private MemoryServiceRepository repository;
    @InjectMocks
    private MemoryServiceImpl memoryService;

    @Test
    void addMessageToConversation_delegatesToRepository() {
        Message message = new Message("q", "a", (short) 0);

        memoryService.addMessageToConversation(CONVERSATION_ID, message);

        verify(repository).addMessageToConversation(CONVERSATION_ID, message);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getConversationHistory_returnsFromRepository() {
        NavigableSet<Message> history = new TreeSet<>();
        history.add(new Message("q", "a", (short) 0));
        when(repository.getConversation(CONVERSATION_ID)).thenReturn(history);

        NavigableSet<Message> result = memoryService.getConversationHistory(CONVERSATION_ID);

        assertThat(result).isEqualTo(history);
        verifyNoMoreInteractions(repository);
    }
}
