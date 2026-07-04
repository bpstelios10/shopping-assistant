package org.learnings.ai.shoppingassistant.domain;

import java.util.UUID;

public record Product(UUID id, String name, String category, float price) {
}
