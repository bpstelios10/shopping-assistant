package org.learnings.ai.shoppingassistant.tools;

import org.learnings.ai.shoppingassistant.domain.Order;
import org.learnings.ai.shoppingassistant.services.orders.OrderService;
import org.learnings.ai.shoppingassistant.services.orders.OrderServiceImpl;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class OrderTool implements OrderAgentTool {

    private final OrderService orderService;

    public OrderTool(OrderService orderService) {
        this.orderService = orderService;
    }

    @Tool(description = "Retrieve an order by its unique order ID. " +
            "Use this tool whenever the user asks about the status, details, or existence of a specific order.")
    public Optional<Order> getOrderById(
            @ToolParam(description = "The unique UUID of the order.") UUID orderId) {
        return orderService.getOrderById(orderId);
    }

//    @Tool(
//            description = "Retrieve all orders placed by a specific user. " +
//                    "Use this tool when the user asks to see their orders or order history."
//    )
//    public List<Order> getOrdersByUserId(
//            @ToolParam(description = "The unique UUID of the user.")
//            UUID userId) {
//        return orderService.getOrdersByUserId(userId);
//    }

    @Tool(
            description = "Create a new customer order. " +
                    "Use this tool only when the user explicitly confirms that they want to place an order."
    )
    public Order createOrder(
            @ToolParam(description = "The information required to create the order, including the customer and products." +
                    "Always include the order-id in the response.")
            OrderServiceImpl.CreateOrderRequest request) {
        return orderService.createOrder(request);
    }
}
