package org.learnings.ai.shoppingassistant.domain;

/**
 * Structured search parameters extracted by the model from the shopper's sentence
 * and passed to the product backend. Optional fields are null when not mentioned.
 */
public record ProductSearchCriteria(String query, Double maxPrice, String category) {
}
