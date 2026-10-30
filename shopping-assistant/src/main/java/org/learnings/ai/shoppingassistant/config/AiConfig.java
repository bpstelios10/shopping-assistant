package org.learnings.ai.shoppingassistant.config;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.learnings.ai.shoppingassistant.advisors.ToolCallAuditingAdvisor;
import org.learnings.ai.shoppingassistant.tools.BoundedToolCallingManager;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionAdvisors;
import org.springframework.ai.session.compaction.ChatModelSummarizer;
import org.springframework.ai.session.compaction.CompactionWindow;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.ai.session.compaction.WindowCompactionStrategy;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class AiConfig {

    @Bean
    ToolCallingManager toolCallingManager(ObservationRegistry observationRegistry,
                                          @Value("${shopping-assistant.tools.max-iterations:5}") int maxIterations) {
        ToolCallingManager delegate = DefaultToolCallingManager.builder()
                .observationRegistry(observationRegistry)
                .build();

        return new BoundedToolCallingManager(delegate, maxIterations);
    }

    // builder beans are singletons. make it prototype to avoid leaks
    @Bean
    @Scope("prototype")
    ChatClient.Builder chatClientBuilderWithChatMemory(ChatModel chatModel,
                                                       SessionService sessionService,
                                                       ObservationRegistry observationRegistry,
                                                       ToolCallAuditingAdvisor toolCallAuditingAdvisor) {
        ChatModelSummarizer summarizer = ChatModelSummarizer.builder(chatModel).build();

        WindowCompactionStrategy summaryStrategy = WindowCompactionStrategy.builder()
                .lastTurns(5)
                .summarizer(summarizer)
                .build();

        // this returns 2 advisors, the compactor and then the memory
        List<Advisor> sessionAdvisorsGroup = SessionAdvisors.builder(sessionService)
                .compaction(new TurnCountTrigger(9), summaryStrategy)
                .build();

        return ChatClient
                .builder(chatModel, observationRegistry, null, null)
                .defaultAdvisors(sessionAdvisorsGroup.getFirst(), sessionAdvisorsGroup.getLast(), toolCallAuditingAdvisor);
    }

    /**
     * Tools call an external backend that can fail (network, NPE, etc.). By default, Spring AI feeds the raw
     * exception text back to the model, which makes it flail and re-open earlier questions. Return a clean,
     * bounded message instead so the model degrades gracefully and stays on the current turn.
     */
    @Bean
    ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        return (ToolExecutionException exception) -> {
            log.warn("tool [{}] failed, returning graceful message to model",
                    exception.getToolDefinition().name(), exception);
            return "This tool is temporarily unavailable. Tell the shopper you couldn't retrieve that "
                    + "information right now and answer only their latest message; do not retry other tools.";
        };
    }

    @Bean
    ChatClient routerChatClient(ChatModel chatModel, ObservationRegistry observationRegistry) {
        ChatOptions.Builder<?> routerChatOptionsBuilder = ChatOptions.builder()
                .temperature(0.0)
                .maxTokens(1000)
                .combineWith(
                        OpenAiChatOptions.builder().extraBody(Map.of("think", false))
                );

        return ChatClient
                .builder(chatModel, observationRegistry, null, null)
                .defaultOptions(routerChatOptionsBuilder)
                .build();
    }

    // thought: could be a tool. tool is better for individual cases. advisors common for ALL agents (like chat memory)
    @Bean
    QuestionAnswerAdvisor ragAdvisor(VectorStore vectorStore) {
        // this is a guard, for 'dumb' models like qwen8. weak instruction-following makes it merge context sometimes
        PromptTemplate qaTemplate = PromptTemplate.builder()
                .template("""
                        Answer ONLY the user's latest question below.
                        Use the context if relevant; ignore it if not.
                        Do not answer previous questions.
                        
                        Context:
                        ---------------------
                        {question_answer_context}
                        ---------------------
                        
                        Latest question: {query}
                        """)
                .build();

        return QuestionAnswerAdvisor.builder(vectorStore)
                .order(2) // runs after: augments only THIS request, not memory
                .promptTemplate(qaTemplate)
                .searchRequest(SearchRequest.builder()
                        .topK(4)
                        .similarityThreshold(0.5)
                        .build())
                .build();
    }

    @Bean
    ObservationFilter agentTagFromChatClientContext() {
        return context -> {
            if (context instanceof ChatClientObservationContext c) {
                String agent = (String) c.getRequest().context().get("agent");

                context.addLowCardinalityKeyValue(KeyValue.of(
                        "agent", (agent == null || agent.isBlank()) ? "unknown" : agent));
            }

            return context;
        };
    }
}
