package org.learnings.ai.shoppingassistant;

import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryRepository;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("component-test")
class ShoppingAssistantApplicationTests {

    @MockitoBean
    private VectorStore vectorStore;
    @MockitoBean
    private ChatMemoryRepository redisChatMemoryRepository;
    @MockitoBean
    private UserMemoryRepository userMemoryRepository;

    @Test
    void contextLoads() { }
}
