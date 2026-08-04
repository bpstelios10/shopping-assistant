package org.learnings.ai.shoppingassistant.infrastructure.orders;

import org.learnings.ai.shoppingassistant.domain.Order;
import org.learnings.ai.shoppingassistant.domain.OrderStatus;

import java.util.UUID;

public record OrderClientResponse(UUID id, String productId, int quantity, OrderStatus status) {

    Order toDomain() {
        return new Order(id, productId, quantity, status);
    }
}
