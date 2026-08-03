package org.learnings.ai.shoppingassistant.services.orders;

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
}
