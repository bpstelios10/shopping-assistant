package org.learnings.ai.shoppingassistant.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.learnings.ai.shoppingassistant.services.dtos.ChatReplyDto;
import org.learnings.ai.shoppingassistant.services.memory.CurrentUser;
import org.learnings.ai.shoppingassistant.tools.AgentTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
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
    public ChatReplyDto chat(String message, String conversationId) {
        conversationId = Strings.isBlank(conversationId) ? UUID.randomUUID().toString() : conversationId;
        final String userId = resolveUserId();
        final String convId = userId + ":" + conversationId;
        log.debug("chat with conversation-id: [{}]", convId);
        CurrentUser.set(userId);

        try {
            ChatResponse chatResponse = chatClient
                    .prompt(promptService.buildShoppingAssistantPrompt(message, userId))
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, convId))
                    .tools(tools.toArray())
                    .call()
                    .chatResponse();

            if (chatResponse == null) {
                throw new RuntimeException("Agent didnt reply");
            }

            return ChatReplyMapper.toChatReplyDto(chatResponse, convId);
        } finally {
            CurrentUser.clear();
        }
    }

    private String resolveUserId() {
        // TODO when i add authentication, store per user - move this to a filter maybe? if authenticated, add user-id in context
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
//            return "user:" + auth.getName(); // or extract sub from JWT
//        }
        // TODO maybe have a sessionId here? and if later the user logs in, the chat history can be stored
        return "anon:sess-abc";
    }
}
