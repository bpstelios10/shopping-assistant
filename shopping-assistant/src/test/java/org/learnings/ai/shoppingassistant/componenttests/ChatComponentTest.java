package org.learnings.ai.shoppingassistant.componenttests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.learnings.ai.shoppingassistant.controllers.ChatController;
import org.learnings.ai.shoppingassistant.services.products.ProductClient;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class ChatComponentTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JsonMapper mapper;
    // TODO maybe switch to org.testcontainers.ollama: https://www.baeldung.com/spring-ai-testing-ai-evaluators
    @MockitoBean
    private OpenAiChatModel chatModel;
    // TODO: use a mock server later.
    @MockitoBean
    private ProductClient productClient;
    @MockitoBean
    private VectorStore vectorStore;
    @MockitoBean
    private RedisChatMemoryRepository redisChatMemoryRepository;

    @BeforeEach
    void setUp() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
    }

    @Test
    void chat_whenCorrectInput_returnsResponse() throws Exception {
        when(chatModel.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("some response")))));

        ChatController.CreateChat request = new ChatController.CreateChat("some message", "some-conversation-id");

        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generations[0].text").value("some response"));
    }

    @Test
    void chat_whenSameConversationId_sendsHistoryToModel() throws Exception {
        when(chatModel.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("first response")))))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("second response")))));
        String conversationId = "history-" + UUID.randomUUID();
        when(redisChatMemoryRepository.findByConversationId("anon:sess-abc:" + conversationId))
                .thenReturn(List.of())
                .thenReturn(List.of(
                        new UserMessage("what is your name"),
                        new AssistantMessage("first response")
                ));

        ChatController.CreateChat first = new ChatController.CreateChat("what is your name", conversationId);
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(first)))
                .andExpect(status().isOk());

        ChatController.CreateChat second = new ChatController.CreateChat("what did i just ask", conversationId);
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(second)))
                .andExpect(status().isOk());

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(2)).call(promptCaptor.capture());
        verify(redisChatMemoryRepository, times(6))
                .findByConversationId("anon:sess-abc:" + conversationId);

        // the second call must carry the first turn (user question + assistant reply) as history.
        // NOTE: the RAG advisor wraps the current user query in its QA template, so we assert
        // against the joined text of all messages (substring match) rather than exact per-message equality.
        Prompt secondPrompt = promptCaptor.getAllValues().get(1);
        String secondPromptText = secondPrompt.getInstructions().stream()
                .map(Message::getText)
                .collect(java.util.stream.Collectors.joining("\n"));
        assertThat(secondPromptText).contains("what is your name", "first response", "what did i just ask");
    }

    @Test
    void chat_whenDifferentConversationId_doesNotShareHistory() throws Exception {
        when(chatModel.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("first response")))))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("second response")))));

        String conversationId1 = UUID.randomUUID().toString();
        String conversationId2 = UUID.randomUUID().toString();
        ChatController.CreateChat first = new ChatController.CreateChat("what is your name", conversationId1);
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(first)))
                .andExpect(status().isOk());

        ChatController.CreateChat second = new ChatController.CreateChat("what did i just ask", conversationId2);
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(second)))
                .andExpect(status().isOk());

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(2)).call(promptCaptor.capture());
        verify(redisChatMemoryRepository, times(3))
                .findByConversationId("anon:sess-abc:" + conversationId1);
        verify(redisChatMemoryRepository, times(3))
                .findByConversationId("anon:sess-abc:" + conversationId2);

        // a different conversation must not leak the first turn
        Prompt secondPrompt = promptCaptor.getAllValues().get(1);
        String secondPromptText = secondPrompt.getInstructions().stream()
                .map(Message::getText)
                .collect(java.util.stream.Collectors.joining("\n"));
        assertThat(secondPromptText)
                .doesNotContain("what is your name", "first response");
    }

    @Test
    void chat_whenRelevantDocsExist_injectsRetrievedContextIntoPrompt() throws Exception {
        when(chatModel.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("some response")))));
        // RAG advisor consults the vector store; return a known chunk so we can assert it is injected.
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("RETRIEVED_RETURN_POLICY_CHUNK")));

        ChatController.CreateChat request = new ChatController.CreateChat("what is your return policy", "rag-" + UUID.randomUUID());
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // the retrieval path must have been consulted and its content fed to the model
        verify(vectorStore).similaritySearch(any(SearchRequest.class));

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        String promptText = promptCaptor.getValue().getInstructions().stream()
                .map(Message::getText)
                .collect(java.util.stream.Collectors.joining("\n"));
        assertThat(promptText).contains("RETRIEVED_RETURN_POLICY_CHUNK");
    }

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

        verifyNoInteractions(chatModel);
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
