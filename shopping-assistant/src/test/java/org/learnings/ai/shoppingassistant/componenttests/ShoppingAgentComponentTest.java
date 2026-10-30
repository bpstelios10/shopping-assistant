package org.learnings.ai.shoppingassistant.componenttests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.agents.RouterAgent;
import org.learnings.ai.shoppingassistant.web.controllers.ChatController;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.session.compaction.ChatModelSummarizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.ExpectedCount;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.learnings.ai.shoppingassistant.agents.RouterAgent.RoutingStep.AgentType.SHOPPING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("component-test")
public class ShoppingAgentComponentTest extends AbstractComponentTestWithMockedExternals {

    @Autowired
    private JsonMapper mapper;
    // task: maybe switch to org.testcontainers.ollama: https://www.baeldung.com/spring-ai-testing-ai-evaluators
    // and then add some scenarios to validate that I actually use productClient.search/getAllProducts
    @MockitoBean
    private ChatClient routerChatClient;
    @MockitoBean
    private OpenAiChatModel chatModel;

    @BeforeEach
    void setUp() {
        when(chatModel.getOptions()).thenReturn(OpenAiChatOptions.builder().build());

        server.reset();
        server.expect(ExpectedCount.max(1), requestTo("http://shopping-assistant/products/categories"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        ["MAGNET","POSTCARD","ACCESSORY","JEWELRY","CLOTHES"]
                        """, MediaType.APPLICATION_JSON));
    }

    @Test
    void chat_whenCorrectInput_returnsResponse() throws Exception {
        String chatMessage = "some message";
        mockModel(chatMessage);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("some response")))));
        String conversationId = "simple-" + UUID.randomUUID();

        ChatController.CreateChat request = new ChatController.CreateChat(chatMessage, conversationId);

        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generations[0].text").value("some response"));

        assertThat(history(conversationId)).containsExactly(
                tuple(USER, chatMessage),
                tuple(ASSISTANT, "some response"));
        verify(chatModel).getOptions();
        verify(chatModel).call(any(Prompt.class));
        verify(userMemoryRepository).findById(USER_ID);
        verify(routerChatClient).prompt();
        verifyNoMoreSuperClassMocksInteractions();
        verifyNoMoreInteractions(chatModel, routerChatClient);
    }

    @Test
    void chat_whenSameConversationId_sendsHistoryToModel() throws Exception {
        String chatMessage1 = "what is your name";
        String chatMessage2 = "what did i just ask";
        mockModel(chatMessage1);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("first response")))))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("second response")))));
        String conversationId = "history-" + UUID.randomUUID();

        ChatController.CreateChat first = new ChatController.CreateChat(chatMessage1, conversationId);
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(first)))
                .andExpect(status().isOk());

        mockModel(chatMessage2);
        ChatController.CreateChat second = new ChatController.CreateChat(chatMessage2, conversationId);
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(second)))
                .andExpect(status().isOk());

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(2)).call(promptCaptor.capture());
        // the second call must carry the first turn (user question + assistant reply) as history
        assertThat(promptText(promptCaptor.getAllValues().get(1)))
                .contains(chatMessage1, "first response", chatMessage2);
        assertThat(history(conversationId)).containsExactly(
                tuple(USER, chatMessage1),
                tuple(ASSISTANT, "first response"),
                tuple(USER, chatMessage2),
                tuple(ASSISTANT, "second response"));
        verify(chatModel, times(2)).getOptions();
        verify(userMemoryRepository, times(2)).findById(USER_ID);
        verify(routerChatClient, times(2)).prompt();
        verifyNoMoreSuperClassMocksInteractions();
        verifyNoMoreInteractions(chatModel, routerChatClient);
    }

    @Test
    void chat_whenDifferentConversationId_doesNotShareHistory() throws Exception {
        String chatMessage1 = "what is your name";
        String chatMessage2 = "what did i just ask";
        mockModel(chatMessage1);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("first response")))))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("second response")))));

        String conversationId1 = UUID.randomUUID().toString();
        String conversationId2 = UUID.randomUUID().toString();
        ChatController.CreateChat first = new ChatController.CreateChat(chatMessage1, conversationId1);
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(first)))
                .andExpect(status().isOk());

        mockModel(chatMessage2);
        ChatController.CreateChat second = new ChatController.CreateChat(chatMessage2, conversationId2);
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(second)))
                .andExpect(status().isOk());

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(2)).call(promptCaptor.capture());
        // a different conversation must not leak the first turn
        assertThat(promptText(promptCaptor.getAllValues().get(1)))
                .doesNotContain(chatMessage1, "first response");
        assertThat(history(conversationId1)).containsExactly(
                tuple(USER, chatMessage1),
                tuple(ASSISTANT, "first response"));
        assertThat(history(conversationId2)).containsExactly(
                tuple(USER, chatMessage2),
                tuple(ASSISTANT, "second response"));
        verify(chatModel, times(2)).getOptions();
        verify(userMemoryRepository, times(2)).findById(USER_ID);
        verify(routerChatClient, times(2)).prompt();
        verifyNoMoreSuperClassMocksInteractions();
        verifyNoMoreInteractions(chatModel, routerChatClient);
    }

    @Test
    void chat_when10TurnsOfQuestionsWithSameConversationId_compactsMessages() throws Exception {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));
        String conversationId = "compact-" + UUID.randomUUID();

        for (int i = 1; i <= 10; i++) {
            String chatMessage = "msg-" + i;
            mockModel(chatMessage);

            ChatController.CreateChat request = new ChatController.CreateChat(chatMessage, conversationId);
            mockMvc.perform(post("/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // 10 chat calls + 1 summary call, made after the 10th turn
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(11)).call(promptCaptor.capture());
        assertThat(promptText(promptCaptor.getAllValues().getLast()))
                .contains("msg-1", "msg-5")
                .doesNotContain("msg-10");
        // the summary turn stands in for turns 1-5, the last 5 turns are kept raw
        assertThat(history(conversationId)).containsExactly(
                tuple(USER, ChatModelSummarizer.DEFAULT_SHADOW_PROMPT), tuple(ASSISTANT, "ok"),
                tuple(USER, "msg-6"), tuple(ASSISTANT, "ok"),
                tuple(USER, "msg-7"), tuple(ASSISTANT, "ok"),
                tuple(USER, "msg-8"), tuple(ASSISTANT, "ok"),
                tuple(USER, "msg-9"), tuple(ASSISTANT, "ok"),
                tuple(USER, "msg-10"), tuple(ASSISTANT, "ok"));
        verify(chatModel, times(10)).getOptions();
        verify(userMemoryRepository, times(10)).findById(USER_ID);
        verify(routerChatClient, times(10)).prompt();
        verifyNoMoreSuperClassMocksInteractions();
        verifyNoMoreInteractions(chatModel, routerChatClient);
    }

    @Test
    void chat_whenUserProfileExists_injectsUserContextIntoPrompt() throws Exception {
        String chatMessage = "do you have jackets?";
        mockModel(chatMessage);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("some response")))));
        when(userMemoryRepository.findById(USER_ID))
                .thenReturn(Optional.of(Map.of("currency", "EUR", "size", "M")));

        String conversationId = "profile-" + UUID.randomUUID();
        ChatController.CreateChat request = new ChatController.CreateChat(chatMessage, conversationId);
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        assertThat(promptText(promptCaptor.getValue()))
                .contains("Known information about the user:")
                .contains("currency=EUR").contains("size=M");
        // the profile is injected per request, never persisted into the conversation
        assertThat(history(conversationId)).containsExactly(
                tuple(USER, chatMessage),
                tuple(ASSISTANT, "some response"));
        verify(chatModel).getOptions();
        verify(userMemoryRepository).findById(USER_ID);
        verify(routerChatClient).prompt();
        verifyNoMoreSuperClassMocksInteractions();
        verifyNoMoreInteractions(chatModel, routerChatClient);
    }

    @SuppressWarnings("unchecked")
    private void mockModel(String message) {
        var reqSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var callSpec = mock(ChatClient.CallResponseSpec.class);

        when(routerChatClient.prompt()).thenReturn(reqSpec);
        when(reqSpec.advisors(any(Consumer.class))).thenReturn(reqSpec);
        when(reqSpec.system(anyString())).thenReturn(reqSpec);
        when(reqSpec.user(message)).thenReturn(reqSpec);
        when(reqSpec.call()).thenReturn(callSpec);
        when(callSpec.entity(RouterAgent.RoutingPlan.class))
                .thenReturn(new RouterAgent.RoutingPlan(List.of(
                        new RouterAgent.RoutingStep(SHOPPING, message, 0.95)
                )));
    }
}
