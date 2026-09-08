package org.learnings.ai.shoppingassistant.agents;

import lombok.extern.slf4j.Slf4j;
import org.learnings.ai.shoppingassistant.agents.prompts.PromptProvider;
import org.learnings.ai.shoppingassistant.tools.ToolContextValues;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.learnings.ai.shoppingassistant.advisors.ToolCallAuditingValues.TOOL_CALLS_CONTEXT_KEY;

/**
 * The single {@link Agent} implementation. Agents differ only in data — name, prompt provider and tools — so they are
 * configured as beans rather than modelled as subclasses.
 */
@Slf4j
public class ChatClientAgent implements Agent {

    private final String name;
    private final ChatClient chatClient;
    private final PromptProvider promptProvider;
    private final Object[] tools;

    public ChatClientAgent(String name, ChatClient chatClient,
                           PromptProvider promptProvider, List<?> tools) {
        this.name = name;
        this.chatClient = chatClient;
        this.promptProvider = promptProvider;
        this.tools = tools.toArray();
    }

    @Override
    public AgentChatResult chat(AgentRequest request) {
        ChatClientResponse response = requestSpec(request)
                .call()
                .chatClientResponse();

        if (response.chatResponse() == null) {
            throw new RuntimeException("Agent [" + name + "] did not respond with a chat response");
        }
        return AgentChatResult.from(response);
    }

    @Override
    public Flux<AgentStreamEvent> chatStream(AgentRequest request) {
        // defer so the per-subscription accumulator is not shared between subscribers, and so building the prompt
        // (which reads user memory) happens on subscribe rather than on assembly.
        return Flux.defer(() -> {
            AtomicReference<ChatResponseMetadata> lastMetadata = new AtomicReference<>();

            return requestSpec(request)
                    .stream()
                    .chatClientResponse()
                    .doOnNext(response -> captureMetadata(response, lastMetadata))
                    .flatMapIterable(this::toEvents)
                    .concatWith(Flux.defer(() -> Flux.just(completed(lastMetadata.get()))));
        });
    }

    @Override
    public String name() {
        return name;
    }

    private ChatClient.ChatClientRequestSpec requestSpec(AgentRequest request) {
        return chatClient
                .prompt(promptProvider.buildPrompt(request.message(), request.userId()))
                .advisors(a -> a
                        .param(ChatMemory.CONVERSATION_ID, request.conversationId())
                        .param("agent", name)
                        // synchronized: under streaming the auditing advisor appends from Reactor threads
                        .param(TOOL_CALLS_CONTEXT_KEY,
                                Collections.synchronizedList(new ArrayList<AssistantMessage.ToolCall>())))
                .toolContext(ToolContextValues.forUser(request.userId()))
                .tools(tools);
    }

    private void captureMetadata(ChatClientResponse response, AtomicReference<ChatResponseMetadata> holder) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null) {
            return;
        }

        // providers send usage only on the final chunk, and some send none at all; keep the richest one seen
        ChatResponseMetadata metadata = chatResponse.getMetadata();
        if (holder.get() == null || hasUsage(metadata)) {
            holder.set(metadata);
        }
    }

    private List<AgentStreamEvent> toEvents(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getResult() == null) {
            return List.of();
        }

        List<AgentStreamEvent> events = new ArrayList<>();
        AssistantMessage output = chatResponse.getResult().getOutput();

        output.getToolCalls().forEach(toolCall -> events.add(new AgentStreamEvent.ToolInvoked(toolCall)));

        String text = output.getText();
        if (text != null && !text.isEmpty()) {
            events.add(new AgentStreamEvent.Delta(text));
        }

        return events;
    }

    /**
     * Normalises "no usage reported" to nulls. Spring AI fills a {@code ChatResponse} that carries no metadata with
     * an empty model and a zero usage, and reporting {@code promptTokens=0} would read as a free call rather than as
     * missing data — some OpenAI-compatible servers never send usage on a stream at all.
     */
    private AgentStreamEvent.Completed completed(ChatResponseMetadata metadata) {
        if (metadata == null) {
            log.debug("agent [{}] finished streaming without metadata", name);
            return new AgentStreamEvent.Completed(null, null, null);
        }

        String model = metadata.getModel().isBlank() ? null : metadata.getModel();
        if (!hasUsage(metadata)) {
            return new AgentStreamEvent.Completed(model, null, null);
        }

        Usage usage = metadata.getUsage();
        return new AgentStreamEvent.Completed(model, usage.getPromptTokens(), usage.getCompletionTokens());
    }

    @SuppressWarnings("all") // suppress the not needed null check
    private boolean hasUsage(ChatResponseMetadata metadata) {
        Usage usage = metadata.getUsage();
        return usage.getTotalTokens() != null && usage.getTotalTokens() > 0;
    }
}
