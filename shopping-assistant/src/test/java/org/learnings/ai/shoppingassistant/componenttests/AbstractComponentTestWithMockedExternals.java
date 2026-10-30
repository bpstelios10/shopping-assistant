package org.learnings.ai.shoppingassistant.componenttests;

import org.assertj.core.groups.Tuple;
import org.learnings.ai.shoppingassistant.config.InMemorySessionConfig;
import org.learnings.ai.shoppingassistant.config.TestRestClientConfig;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.autoconfigure.exclude=org.springframework.ai.model.session.jdbc.autoconfigure.JdbcSessionRepositoryAutoConfiguration")
@AutoConfigureMockMvc
@Import({TestRestClientConfig.class, InMemorySessionConfig.class})
public abstract class AbstractComponentTestWithMockedExternals {

    static final String USER_ID = "anon:sess-abc";

    @MockitoBean
    VectorStore vectorStore;
    @MockitoBean
    DataSource dataSource;
    @MockitoBean
    UserMemoryRepository userMemoryRepository;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MockRestServiceServer server;
    @Autowired
    SessionService sessionService;

    void verifyNoMoreSuperClassMocksInteractions() {
        verifyNoMoreInteractions(vectorStore, userMemoryRepository);
    }

    /**
     * The active session view (what the next prompt is built from) as (type, text) tuples.
     */
    List<Tuple> history(String conversationId) {
        return sessionService.getEvents(USER_ID + ":" + conversationId, EventFilter.active()).stream()
                .filter(SessionEvent::hasMessage)
                .map(event -> tuple(event.getMessageType(), event.getMessage().getText()))
                .toList();
    }

    static String promptText(Prompt prompt) {
        return prompt.getInstructions().stream()
                .map(Message::getText)
                .collect(Collectors.joining("\n"));
    }
}
