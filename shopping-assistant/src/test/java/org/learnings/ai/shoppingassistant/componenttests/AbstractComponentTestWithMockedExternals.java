package org.learnings.ai.shoppingassistant.componenttests;

import org.learnings.ai.shoppingassistant.services.memory.UserMemoryRepository;
import org.learnings.ai.shoppingassistant.services.orders.OrderClient;
import org.learnings.ai.shoppingassistant.services.products.ProductClient;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class AbstractComponentTestWithMockedExternals {

    @MockitoBean
    VectorStore vectorStore;
    @MockitoBean
    DataSource dataSource;
    @MockitoBean
    RedisChatMemoryRepository redisChatMemoryRepository;
    @MockitoBean
    UserMemoryRepository userMemoryRepository;

    @Autowired
    MockMvc mockMvc;
    // Mock the product backend so the context doesn't need the real Go service.
    @MockitoBean
    ProductClient productClient;
    @MockitoBean
    OrderClient orderClient;
}
