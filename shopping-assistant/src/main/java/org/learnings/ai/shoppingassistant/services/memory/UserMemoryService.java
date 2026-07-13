package org.learnings.ai.shoppingassistant.services.memory;

import java.util.Map;
import java.util.Optional;

public interface UserMemoryService {

    /** Returns a short human-readable summary of stored user preferences, or empty if none. */
    Optional<String> getProfileSummary(String userId);

    /** Merges the given fields into the user's stored profile. */
    void updateProfile(String userId, Map<String, Object> updates);
}
