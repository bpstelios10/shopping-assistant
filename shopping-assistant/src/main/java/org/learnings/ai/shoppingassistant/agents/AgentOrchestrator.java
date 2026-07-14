package org.learnings.ai.shoppingassistant.agents;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.learnings.ai.shoppingassistant.services.memory.CurrentUser;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class AgentOrchestrator {

    private final List<Agent> agents;

    public AgentOrchestrator(List<Agent> agents) {
        this.agents = agents;
    }

    public ChatResponse chat(String message, String conversationId) {
        conversationId = Strings.isBlank(conversationId) ? UUID.randomUUID().toString() : conversationId;
        final String userId = resolveUserId();
        final String convId = userId + ":" + conversationId;
        log.debug("chat with conversation-id: [{}]", convId);
        CurrentUser.set(userId);

        try {
            return route(message).chat(message, convId);
        } finally {
            CurrentUser.clear();
        }
    }

    // TODO step 1: single agent. Later: replace with a planner/router (LLM or classifier) that picks the agent.
    private Agent route(String message) {
        return agents.getFirst();
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
