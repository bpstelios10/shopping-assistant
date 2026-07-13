package org.learnings.ai.shoppingassistant.services.memory;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserMemoryServiceImpl implements UserMemoryService {

    private final UserMemoryRepository repository;

    public UserMemoryServiceImpl(UserMemoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<String> getProfileSummary(String userId) {
        // TODO add a call to get user profile? like name, address etc?
        return repository.findById(userId)
                .filter(profile -> !profile.isEmpty())
                .map(profile -> profile.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(", ")));
    }

    @Override
    public void updateProfile(String userId, Map<String, Object> updates) {
        Map<String, Object> existing = repository.findById(userId)
                .map(HashMap::new)
                .orElseGet(HashMap::new);
        existing.putAll(updates);
        repository.save(userId, existing);
    }
}
