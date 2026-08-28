package org.learnings.ai.shoppingassistant.services.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMemoryServiceImplTest {

    @Mock
    private UserMemoryRepository repository;
    @InjectMocks
    private UserMemoryServiceImpl service;

    @Test
    void getProfileSummary_whenNoProfile_returnsEmpty() {
        when(repository.findById("u1")).thenReturn(Optional.empty());

        assertThat(service.getProfileSummary("u1")).isEmpty();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getProfileSummary_whenProfileIsEmpty_returnsEmpty() {
        when(repository.findById("u1")).thenReturn(Optional.of(Map.of()));

        assertThat(service.getProfileSummary("u1")).isEmpty();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getProfileSummary_whenProfileHasData_returnsSummaryString() {
        when(repository.findById("u1")).thenReturn(Optional.of(Map.of("currency", "EUR", "size", "M")));

        Optional<String> result = service.getProfileSummary("u1");

        assertThat(result).isPresent();
        assertThat(result.get()).contains("currency=EUR").contains("size=M");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void updateProfile_delegatesToRepository() {
        doNothing().when(repository).merge("u1", Map.of("currency", "EUR"));

        service.updateProfile("u1", Map.of("currency", "EUR"));

        verifyNoMoreInteractions(repository);
    }
}
