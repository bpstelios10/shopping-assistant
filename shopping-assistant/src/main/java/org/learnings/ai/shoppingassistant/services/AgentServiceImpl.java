package org.learnings.ai.shoppingassistant.services;

import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.learnings.ai.shoppingassistant.services.dtos.Message;
import org.learnings.ai.shoppingassistant.tools.AgentTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NavigableSet;

@Service
public class AgentServiceImpl implements AgentService {

    private final ChatClient chatClient;
    private final MemoryService memoryService;
    private final PromptService promptService;
    private final List<AgentTool> tools;

    public AgentServiceImpl(ChatClient chatClient, MemoryService memoryService, PromptService promptService,
                            List<AgentTool> tools) {
        this.chatClient = chatClient;
        this.memoryService = memoryService;
        this.promptService = promptService;
        this.tools = tools;
    }

    @Override
    public ChatReplyDto chat(String message, String conversationId) {
        NavigableSet<Message> conversationHistory = memoryService.getConversationHistory(conversationId);

        ChatResponse chatResponse = chatClient
                .prompt(promptService.buildShoppingAssistantPrompt(message, conversationHistory))
                .tools(tools.toArray())
                .call()
                .chatResponse();

        if (chatResponse == null) {
            throw new RuntimeException("Agent didnt reply");
        }

        memoryService.addMessageToConversation(conversationId,
                new Message(message, chatResponse.getResult().getOutput().getText(), (short) conversationHistory.size()));

        return ChatReplyMapper.toChatReplyDto(chatResponse);
    }
}
