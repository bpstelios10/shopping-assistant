package org.learnings.ai.shoppingassistant.services.orders;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.learnings.ai.shoppingassistant.domain.Order;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderClient orderClient;

    public OrderServiceImpl(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    @Override
    public Optional<Order> getOrderById(UUID orderId) {
        return orderClient.getOrderById(orderId);
    }

    @Override
    public Order createOrder(CreateOrderRequest request) {
        return orderClient.createOrder(request);
    }

    public record CreateOrderRequest(@JsonProperty("product_id") String productId, int quantity) { }
}
