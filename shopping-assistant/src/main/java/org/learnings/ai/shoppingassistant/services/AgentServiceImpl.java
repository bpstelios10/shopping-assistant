package org.learnings.ai.shoppingassistant.services;

import org.jspecify.annotations.NonNull;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.learnings.ai.shoppingassistant.tools.AgentTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentServiceImpl implements AgentService {

    private final ChatClient chatClient;
    private final PromptService promptService;
    private final List<AgentTool> tools;

    public AgentServiceImpl(ChatClient chatClient, PromptService promptService, List<AgentTool> tools) {
        this.chatClient = chatClient;
        this.promptService = promptService;
        this.tools = tools;
    }

    @Override
    public ChatReplyDto chat(String message, @NonNull String conversationId) {
        ChatResponse chatResponse = chatClient
                .prompt(promptService.buildShoppingAssistantPrompt(message))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(tools.toArray())
                .call()
                .chatResponse();

        if (chatResponse == null) {
            throw new RuntimeException("Agent didnt reply");
        }

        return ChatReplyMapper.toChatReplyDto(chatResponse);
    }
}
