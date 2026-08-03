package org.learnings.ai.shoppingassistant.services.orders;

import org.learnings.ai.shoppingassistant.domain.Order;

import java.util.Optional;
import java.util.UUID;

/**
 * Client to the product backend (a separate Go service).
 */
public interface OrderClient {

    Optional<Order> getOrderById(UUID orderId);
}
