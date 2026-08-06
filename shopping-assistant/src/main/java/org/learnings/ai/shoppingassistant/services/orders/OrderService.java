package org.learnings.ai.shoppingassistant.services.orders;

import org.learnings.ai.shoppingassistant.domain.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderService {

    Optional<Order> getOrderById(UUID orderId);

    Order createOrder(OrderServiceImpl.CreateOrderRequest request);
}
