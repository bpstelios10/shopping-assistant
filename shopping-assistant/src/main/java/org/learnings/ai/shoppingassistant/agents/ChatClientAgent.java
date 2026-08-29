package org.learnings.ai.shoppingassistant.agents;

import org.learnings.ai.shoppingassistant.agents.prompts.PromptProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.List;

import static org.learnings.ai.shoppingassistant.advisors.ToolCallAuditingValues.TOOL_CALLS_CONTEXT_KEY;

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
    public AgentChatResult chat(String message, String conversationId) {
        ChatClientResponse response = chatClient
                .prompt(promptProvider.buildPrompt(message))
                .advisors(a -> a
                        .param(ChatMemory.CONVERSATION_ID, conversationId)
                        .param("agent", name)
                        .param(TOOL_CALLS_CONTEXT_KEY, new ArrayList<AssistantMessage.ToolCall>()))
                .tools(tools)
                .call()
                .chatClientResponse();

        if (response.chatResponse() == null) {
            throw new RuntimeException("Agent [" + name + "] did not respond with a chat response");
        }
        return AgentChatResult.from(response);
    }

    @Override
    public String name() {
        return name;
    }
}
