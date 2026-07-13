package org.learnings.ai.shoppingassistant.infrastructure.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcUserMemoryRepository implements UserMemoryRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcUserMemoryRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Map<String, Object>> findById(String userId) {
        return jdbc.query(
                "SELECT profile FROM user_memory WHERE user_id = ?",
                rs -> rs.next()
                        ? Optional.of(parse(rs.getString("profile")))
                        : Optional.empty(),
                userId
        );
    }

    @Override
    public void save(String userId, Map<String, Object> profile) {
        jdbc.update("""
                INSERT INTO user_memory (user_id, profile, updated_at)
                VALUES (?, ?::jsonb, now())
                ON CONFLICT (user_id) DO UPDATE
                SET profile = EXCLUDED.profile, updated_at = now()
                """,
                userId, serialize(profile));
    }

    private Map<String, Object> parse(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    private String serialize(Map<String, Object> profile) {
        try {
            return objectMapper.writeValueAsString(profile);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
