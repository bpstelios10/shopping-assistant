package org.learnings.ai.shoppingassistant.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RouterAgent {

    private final ChatClient chatClient;

    public RouterAgent(ChatClient routerChatClient) {
        this.chatClient = routerChatClient;
    }

    public RoutingPlan route(String message) {
        // task: later add some Cheap pre-filters, like hello messages, very obvious ones, block abuse, etc
        return chatClient.prompt()
                .system("""
                        You are a routing planner for an online store.
                        
                        Analyze the user's request and split it into one or more independent tasks.
                        
                        For each task:
                        - Select the most appropriate agent.
                        - Rewrite the task so it can be executed independently.
                        - Assign a confidence between 0.0 and 1.0.
                        
                        Available agents:
                        
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
                        
                        Return the tasks in execution order.
                        Do not return duplicate tasks.
                        """)
                .user(message)
                .call()
                .entity(RoutingPlan.class);
    }

    public record RoutingPlan(List<RoutingStep> decisions) {}

    // task: is there a value of adding reasoning here as well? as third param
    public record RoutingStep(AgentType agent, String task, double confidence) {
        public enum AgentType {SHOPPING, ORDERS, SUPPORT}
    }
}
