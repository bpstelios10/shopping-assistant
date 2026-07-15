package org.learnings.ai.shoppingassistant.componenttests;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryRepository;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class ChatEndpointInputValidationComponentTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private VectorStore vectorStore;
    @MockitoBean
    private RedisChatMemoryRepository redisChatMemoryRepository;
    @MockitoBean
    private UserMemoryRepository userMemoryRepository;

    @ParameterizedTest
    @MethodSource("invalidRequestBodies")
    void chat_whenInvalidBody_returnsBadRequest(String body, String errorMessage) throws Exception {
        MockHttpServletRequestBuilder request = post("/chat")
                .contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request.content(body);
        }

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(content().string(errorMessage));

        verifyNoInteractions(vectorStore, redisChatMemoryRepository, userMemoryRepository);
    }

    private static Stream<Arguments> invalidRequestBodies() {
        return Stream.of(
                arguments(named("missing body", null), "{\"detail\":\"Failed to read request\",\"instance\":\"/chat\",\"status\":400,\"title\":\"Bad Request\"}"),
                arguments(named("null message", "{}"), "{\"errorCode\":\"INVALID_USER_INPUT\",\"errorMessage\":\"message: must not be blank\"}"),
                arguments(named("empty message", "{\"message\":\"\"}"), "{\"errorCode\":\"INVALID_USER_INPUT\",\"errorMessage\":\"message: must not be blank\"}"),
                arguments(named("blank message", "{\"message\":\"  \"}"), "{\"errorCode\":\"INVALID_USER_INPUT\",\"errorMessage\":\"message: must not be blank\"}")
        );
    }
}
