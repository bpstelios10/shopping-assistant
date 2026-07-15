package org.learnings.ai.shoppingassistant.agents;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.learnings.ai.shoppingassistant.services.memory.CurrentUser;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AgentOrchestrator {

    private static final double ROUTING_THRESHOLD = 0.6;

    private final Map<String, Agent> agentsByName;
    private final RouterAgent routerAgent;

    public AgentOrchestrator(List<Agent> agents, RouterAgent routerAgent) {
        this.agentsByName = agents.stream()
                .collect(Collectors.toMap(Agent::name, a -> a));
        this.routerAgent = routerAgent;
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

    private Agent route(String message) {
        RouterAgent.RoutingDecision decision = routerAgent.route(message);
        log.debug("routing decision: {} ({})", decision.agent(), decision.confidence());

        Agent fallbackAgent = agentsByName.get("shopping");
        if (fallbackAgent == null) {
            throw new IllegalStateException("No fallback 'shopping' agent configured");
        }

        if (decision.confidence() < ROUTING_THRESHOLD) {
            return fallbackAgent;
        }

        return agentsByName.getOrDefault(decision.agent().name().toLowerCase(), fallbackAgent);
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
