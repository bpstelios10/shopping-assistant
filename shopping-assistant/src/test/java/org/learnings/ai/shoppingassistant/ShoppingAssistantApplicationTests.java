package org.learnings.ai.shoppingassistant;

import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryRepository;
import org.springframework.ai.model.session.jdbc.autoconfigure.JdbcSessionRepositoryAutoConfiguration;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("component-test")
@EnableAutoConfiguration(exclude = {JdbcSessionRepositoryAutoConfiguration.class})
class ShoppingAssistantApplicationTests {

    @MockitoBean
    private VectorStore vectorStore;
    @MockitoBean
    private SessionService sessionService;
    @MockitoBean
    private UserMemoryRepository userMemoryRepository;

    @Test
    void contextLoads() {
    }
}
