package org.learnings.ai.shoppingassistant.services.memory;

import java.util.Map;
import java.util.Optional;

public interface UserMemoryRepository {

    Optional<Map<String, Object>> findById(String userId);

    /**
     * Atomically merges the given fields into the stored profile, creating it if absent.
     * Existing keys are overwritten, keys not present in {@code updates} are left untouched.
     * Shallow merge — nested objects are replaced, not merged.
     */
    void merge(String userId, Map<String, Object> profile);
}
