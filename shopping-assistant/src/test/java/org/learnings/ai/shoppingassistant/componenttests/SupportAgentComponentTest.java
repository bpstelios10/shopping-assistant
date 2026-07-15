package org.learnings.ai.shoppingassistant.componenttests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.agents.RouterAgent;
import org.learnings.ai.shoppingassistant.controllers.ChatController;
import org.learnings.ai.shoppingassistant.services.memory.UserMemoryRepository;
import org.learnings.ai.shoppingassistant.services.products.ProductClient;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
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
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.learnings.ai.shoppingassistant.agents.RouterAgent.RoutingDecision.AgentType.SUPPORT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class SupportAgentComponentTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JsonMapper mapper;
    // TODO maybe switch to org.testcontainers.ollama: https://www.baeldung.com/spring-ai-testing-ai-evaluators
    @MockitoBean
    private ChatClient routerChatClient;
    @MockitoBean
    private OpenAiChatModel chatModel;
    // TODO: use a mock server later.
    @MockitoBean
    private ProductClient productClient;
    @MockitoBean
    private VectorStore vectorStore;
    @MockitoBean
    private RedisChatMemoryRepository redisChatMemoryRepository;
    @MockitoBean
    private UserMemoryRepository userMemoryRepository;

    @BeforeEach
    void setUp() {
        when(chatModel.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        var reqSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var callSpec = mock(ChatClient.CallResponseSpec.class);

        when(routerChatClient.prompt()).thenReturn(reqSpec);
        when(reqSpec.system(anyString())).thenReturn(reqSpec);
        when(reqSpec.user(anyString())).thenReturn(reqSpec);
        when(reqSpec.call()).thenReturn(callSpec);
        when(callSpec.entity(RouterAgent.RoutingDecision.class))
                .thenReturn(new RouterAgent.RoutingDecision(SUPPORT, 0.95));
    }

    @Test
    void chat_whenCorrectInput_returnsResponse() throws Exception {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("some response")))));

        ChatController.CreateChat request = new ChatController.CreateChat("some message", "some-conversation-id");

        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generations[0].text").value("some response"));
        verify(chatModel).getOptions();
        verify(chatModel).call(any(Prompt.class));
        verify(redisChatMemoryRepository, times(3)).findByConversationId("anon:sess-abc:some-conversation-id");
        verify(redisChatMemoryRepository, times(2)).saveAll(eq("anon:sess-abc:some-conversation-id"), any());
        verify(userMemoryRepository).findById("anon:sess-abc");
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
        verifyNoMoreInteractions(chatModel, productClient, vectorStore, redisChatMemoryRepository, userMemoryRepository);
    }

    @Test
    void chat_whenSameConversationId_sendsHistoryToModel() throws Exception {
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
        verify(chatModel, times(2)).getOptions();
        verify(chatModel, times(2)).call(any(Prompt.class));
        verify(vectorStore, times(2)).similaritySearch(any(SearchRequest.class));
        verify(redisChatMemoryRepository, times(6)).findByConversationId("anon:sess-abc:" + conversationId);
        verify(redisChatMemoryRepository, times(4)).saveAll(eq("anon:sess-abc:" + conversationId), any());
        verify(userMemoryRepository, times(2)).findById("anon:sess-abc");
        verifyNoMoreInteractions(chatModel, productClient, vectorStore, redisChatMemoryRepository, userMemoryRepository);
    }

    @Test
    void chat_whenDifferentConversationId_doesNotShareHistory() throws Exception {
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
        verify(chatModel, times(2)).getOptions();
        verify(chatModel, times(2)).call(any(Prompt.class));
        verify(vectorStore, times(2)).similaritySearch(any(SearchRequest.class));
        verify(redisChatMemoryRepository, times(3)).findByConversationId("anon:sess-abc:" + conversationId1);
        verify(redisChatMemoryRepository, times(3)).findByConversationId("anon:sess-abc:" + conversationId2);
        verify(redisChatMemoryRepository, times(2)).saveAll(eq("anon:sess-abc:" + conversationId1), any());
        verify(redisChatMemoryRepository, times(2)).saveAll(eq("anon:sess-abc:" + conversationId2), any());
        verify(userMemoryRepository, times(2)).findById("anon:sess-abc");
        verifyNoMoreInteractions(chatModel, productClient, vectorStore, redisChatMemoryRepository, userMemoryRepository);
    }

    @Test
    void chat_whenRelevantDocsExist_injectsRetrievedContextIntoPrompt() throws Exception {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("some response")))));
        // RAG advisor consults the vector store; return a known chunk so we can assert it is injected.
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("RETRIEVED_RETURN_POLICY_CHUNK")));

        String conversationId = "rag-" + UUID.randomUUID();
        ChatController.CreateChat request = new ChatController.CreateChat("what is your return policy", conversationId);
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
        verify(chatModel).getOptions();
        verify(chatModel).call(any(Prompt.class));
        verify(redisChatMemoryRepository, times(3)).findByConversationId("anon:sess-abc:" + conversationId);
        verify(redisChatMemoryRepository, times(2)).saveAll(eq("anon:sess-abc:" + conversationId), any());
        verify(userMemoryRepository).findById("anon:sess-abc");
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
        verifyNoMoreInteractions(chatModel, productClient, vectorStore, redisChatMemoryRepository, userMemoryRepository);
    }

    @SuppressWarnings("unchecked")
    @Test
    void chat_when10TurnsOfQuestionsWithSameConversationId_compactsMessages() throws Exception {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));

        String conversationId = "compact-" + UUID.randomUUID();
        String fullConversationId = "anon:sess-abc:" + conversationId;

        Map<String, List<Message>> redisStore = new ConcurrentHashMap<>();
        when(redisChatMemoryRepository.findByConversationId(fullConversationId))
                .thenAnswer(_ -> redisStore.getOrDefault(fullConversationId, List.of()));
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            List<Message> messages = invocation.getArgument(1);
            redisStore.put(id, List.copyOf(messages));
            return null;
        }).when(redisChatMemoryRepository).saveAll(any(String.class), any(List.class));

        for (int i = 1; i <= 10; i++) {
            ChatController.CreateChat request = new ChatController.CreateChat("msg-" + i, conversationId);
            mockMvc.perform(post("/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        List<Message> persisted = redisStore.get(fullConversationId);
        assertThat(persisted).isNotNull();
        assertThat(persisted).hasSize(11);
        assertThat(persisted.getFirst()).isInstanceOf(SystemMessage.class);
        assertThat(persisted.getFirst().getText()).startsWith("Summary of earlier conversation:\n");

        List<String> rawTexts = persisted.stream()
                .skip(1)
                .map(Message::getText)
                .toList();
        assertThat(rawTexts).hasSize(10).contains("msg-10").doesNotContain("msg-1");
        verify(chatModel, times(10)).getOptions();
        verify(chatModel, times(11)).call(any(Prompt.class));
        verify(vectorStore, times(10)).similaritySearch(any(SearchRequest.class));
        verify(redisChatMemoryRepository, times(30)).findByConversationId("anon:sess-abc:" + conversationId);
        verify(redisChatMemoryRepository, times(20)).saveAll(eq("anon:sess-abc:" + conversationId), any());
        verify(userMemoryRepository, times(10)).findById("anon:sess-abc");
        verifyNoMoreInteractions(chatModel, productClient, vectorStore, redisChatMemoryRepository, userMemoryRepository);
    }

    @Test
    void chat_whenUserProfileExists_injectsUserContextIntoPrompt() throws Exception {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("some response")))));
        when(userMemoryRepository.findById("anon:sess-abc"))
                .thenReturn(java.util.Optional.of(Map.of("currency", "EUR", "size", "M")));

        UUID conversationId = UUID.randomUUID();
        ChatController.CreateChat request =
                new ChatController.CreateChat("do you have jackets?", "profile-" + conversationId);
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        String promptText = promptCaptor.getValue().getInstructions().stream()
                .map(Message::getText)
                .collect(java.util.stream.Collectors.joining("\n"));
        assertThat(promptText)
                .contains("Known information about the user:")
                .contains("currency=EUR").contains("size=M");
        verify(chatModel).getOptions();
        verify(chatModel).call(any(Prompt.class));
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
        verify(redisChatMemoryRepository, times(3)).findByConversationId("anon:sess-abc:profile-" + conversationId);
        verify(redisChatMemoryRepository, times(2)).saveAll(eq("anon:sess-abc:profile-" + conversationId), any());
        verify(userMemoryRepository).findById("anon:sess-abc");
        verifyNoMoreInteractions(chatModel, productClient, vectorStore, redisChatMemoryRepository, userMemoryRepository);
    }
}
