package org.learnings.ai.shoppingassistant.infrastructure.products;

import org.learnings.ai.shoppingassistant.domain.Product;

import java.util.UUID;

public record ProductClientResponse(String id, String name, String category, double price) {

    Product toDomain() {
        return new Product(id == null ? null : UUID.fromString(id), name, category, (float) price);
    }
}
