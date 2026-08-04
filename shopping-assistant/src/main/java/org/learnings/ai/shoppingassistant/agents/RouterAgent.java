package org.learnings.ai.shoppingassistant.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

// TODO this agent consumes WAY TOO MANY tokens. this is probably cause i use qwen when there is no reason for
// over-explanations and over-thinking etc. OR it could be cause of the structured response. or both... investigate
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
                        
                        Choose exactly one agent.
                        
                        SHOPPING:
                        - Product search
                        - Product recommendations
                        - Product comparisons
                        - Product information
                        
                        ORDERS:
                        - Place an order
                        - Order status
                        - Order history
                        - Cancel an order
                        
                        SUPPORT:
                        - Refunds
                        - Shipping
                        - Returns
                        - Warranty
                        - FAQs
                        - Store policies
                        
                        Return only the selected agent and a confidence between 0.0 and 1.0.
                        """)
                .user(message)
                .call()
                .entity(RoutingDecision.class);
    }

    // TODO is there a value of adding reasoning here as well? as third param
    public record RoutingDecision(AgentType agent, double confidence) {
        public enum AgentType {SHOPPING, ORDERS, SUPPORT}
    }
}
