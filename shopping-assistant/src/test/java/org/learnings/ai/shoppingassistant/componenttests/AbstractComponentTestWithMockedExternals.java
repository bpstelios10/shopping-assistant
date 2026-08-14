package org.learnings.ai.shoppingassistant.componenttests;

import org.learnings.ai.shoppingassistant.config.TestRestClientConfig;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryRepository;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestRestClientConfig.class)
public abstract class AbstractComponentTestWithMockedExternals {

    @MockitoBean
    VectorStore vectorStore;
    @MockitoBean
    DataSource dataSource;
    @MockitoBean
    ChatMemoryRepository redisChatMemoryRepository;
    @MockitoBean
    UserMemoryRepository userMemoryRepository;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MockRestServiceServer server;

    void verifyNoMoreSuperClassMocksInteractions() {
        verifyNoMoreInteractions(vectorStore, redisChatMemoryRepository, userMemoryRepository);
    }
}
