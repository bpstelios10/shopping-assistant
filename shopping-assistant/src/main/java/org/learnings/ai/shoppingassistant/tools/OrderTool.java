package org.learnings.ai.shoppingassistant.tools;

import org.learnings.ai.shoppingassistant.domain.Order;
import org.learnings.ai.shoppingassistant.services.orders.OrderService;
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
}
