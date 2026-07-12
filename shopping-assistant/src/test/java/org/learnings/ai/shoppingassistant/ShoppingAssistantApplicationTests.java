package org.learnings.ai.shoppingassistant;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ShoppingAssistantApplicationTests {

    @MockitoBean
    private VectorStore vectorStore;
    @MockitoBean
    private RedisChatMemoryRepository redisChatMemoryRepository;

    @Test
    void contextLoads() {
    }
}

