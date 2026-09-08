package org.learnings.ai.shoppingassistant.agents;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    public AgentChatResult chat(String message, String conversationId) {
        String convId = conversationId(conversationId);
        String userId = resolveUserId();

        RouterAgent.RoutingStep decision = route(message);

        return getAgent(decision).chat(new AgentRequest(decision.task(), convId, userId));
    }

    /**
     * Streaming counterpart of {@link #chat(String, String)}.
     *
     * <p>Routing is a full blocking inference, so it is pushed onto {@link Schedulers#boundedElastic()} rather than
     * run on the servlet thread: with streaming the caller expects the response to be committed immediately, and
     * blocking here would hold a container thread for the whole routing round-trip.
     *
     * <p>The routing decision is published as the first event so a client can render "asking the orders agent…"
     * before any token arrives.
     */
    public Flux<AgentStreamEvent> chatStream(String message, String conversationId) {
        String convId = conversationId(conversationId);
        String userId = resolveUserId();

        return Mono.fromCallable(() -> route(message))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(decision -> {
                    Agent agent = getAgent(decision);

                    return Flux.<AgentStreamEvent>just(
                                    new AgentStreamEvent.Routed(agent.name(), convId, decision.confidence()))
                            .concatWith(agent.chatStream(new AgentRequest(decision.task(), convId, userId)));
                });
    }

    private RouterAgent.RoutingStep route(String message) {
        RouterAgent.RoutingPlan plan = routerAgent.route(message);
        log.debug("route plan: [{}]", plan);

        // task: handle the multiple decisions
        List<RouterAgent.RoutingStep> decisions = plan.decisions();

        // if no decision -> fallback agent (0 confidence) with the whole user message as task
        return decisions.isEmpty()
                ? new RouterAgent.RoutingStep(null, message, 0d)
                : decisions.getFirst();
    }

    private String conversationId(String conversationId) {
        String id = !StringUtils.hasText(conversationId) ? UUID.randomUUID().toString() : conversationId;
        String convId = resolveUserId() + ":" + id;
        log.debug("chat with conversation-id: [{}]", convId);

        return convId;
    }

    private Agent getAgent(RouterAgent.RoutingStep decision) {
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
        // task: when i add authentication, store per user - move this to a filter maybe? if authenticated, add user-id in context
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
//            return "user:" + auth.getName(); // or extract sub from JWT
//        }
        // task: have a sessionId here? and if later the user logs in, the chat history can be stored
        return "anon:sess-abc";
    }
}
