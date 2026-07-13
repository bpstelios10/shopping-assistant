package org.learnings.ai.shoppingassistant.services.memory;

import java.util.Map;
import java.util.Optional;

public interface UserMemoryRepository {

    Optional<Map<String, Object>> findById(String userId);

    void save(String userId, Map<String, Object> profile);
}
