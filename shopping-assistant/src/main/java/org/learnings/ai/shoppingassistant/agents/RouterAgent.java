package org.learnings.ai.shoppingassistant.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class RouterAgent {

    private final ChatClient chatClient;

    public RouterAgent(ChatClient routerChatClient) {
        this.chatClient = routerChatClient;
    }

    public RoutingDecision route(String message) {
        // TODO later add some Cheap pre-filters, like hello messages, very obvious ones, block abuse, etc
        return chatClient.prompt()
                .system("""
                        You are a routing classifier for an online store.
                        Choose the best agent to handle the user's message.
                        SHOPPING: product discovery, availability, prices, recommendations.
                        SUPPORT: returns, refunds, shipping, delivery, cancellations, policies.
                        Return the agent and a confidence 0.0-1.0.
                        """)
                .user(message)
                .call()
                .entity(RoutingDecision.class);
    }

    public record RoutingDecision(AgentType agent, double confidence) {
        public enum AgentType {SHOPPING, SUPPORT}
    }
}
