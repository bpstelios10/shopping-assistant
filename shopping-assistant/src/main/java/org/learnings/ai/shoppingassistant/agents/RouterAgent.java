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
                        
                        Analyze the user's request and produce the minimum number of agent calls required.
                        If the latest user message provides missing information for a previously identified task, continue that task instead of creating a new one.
                        
                        Each routing step corresponds to exactly one agent invocation.
                        
                        Only create multiple steps when different agents are required, or
                        the requests are truly independent and cannot reasonably be answered in a single call.
                        
                        If multiple requests: require the same agent, depend on one another, or can reasonably be answered together,
                        they MUST be merged into a single task.
                        
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
                        
                        Choose the agent by the user's INTENT, not by which nouns appear in the message.
                        A product name inside an order request does NOT require a SHOPPING step.
                        
                        Return the tasks in execution order.
                        Do not return duplicate tasks.
                        
                        EXAMPLES:
                        USER: what is my order X status? and what is the return policy?
                        Return:
                        {
                          "decisions": [
                            {
                              "agent": "ORDERS",
                              "task": "what is my order X status?",
                              "confidence": X
                            },
                            {
                              "agent": "SUPPORT",
                              "task": "what is the return policy?",
                              "confidence": X
                            }
                          ]
                        }
                        
                        User: do you have any macbooks? if yes, what is the price?
                        This is multiple requests. But they can be merged and return:
                        {
                          "decisions": [
                            {
                              "agent": "SHOPPING",
                              "task": "do you have any macbooks? if yes, what is the price?",
                              "confidence": X
                            }
                          ]
                        }
                        
                        User: can u tell me all the categories of products u sell and explain the categories?
                        This is multiple requests. But they can be merged and return:
                        {
                          "decisions": [
                            {
                              "agent": "SHOPPING",
                              "task": "can u tell me all the categories of products u sell and explain the categories?",
                              "confidence": X
                            }
                          ]
                        }
                        
                        User: "What categories of products do you sell and explain each category."
                        Return:
                        {
                          "decisions": [
                            {
                              "agent": "SHOPPING",
                              "task": "What categories of products do you sell and explain each category?",
                              "confidence": X
                            }
                          ]
                        }
                        
                        User: "Compare the iPhone 17 and Pixel 11 and recommend one."
                        Return:
                        {
                          "decisions": [
                            {
                              "agent": "SHOPPING",
                              "task": "Compare the iPhone 17 and Pixel 11 and recommend one.",
                              "confidence": X
                            }
                          ]
                        }
                        """)
                .user(message)
                .call()
                .entity(RoutingPlan.class);
    }

    public record RoutingPlan(List<RoutingStep> decisions) {
    }

    // task: is there a value of adding reasoning here as well? as third param
    public record RoutingStep(AgentType agent, String task, double confidence) {
        public enum AgentType {SHOPPING, ORDERS, SUPPORT}
    }
}
