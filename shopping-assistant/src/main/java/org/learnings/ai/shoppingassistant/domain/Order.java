package org.learnings.ai.shoppingassistant.domain;

import java.util.UUID;

public record Order(UUID id, String productId, int quantity, OrderStatus status) {
}
