package org.learnings.ai.shoppingassistant.infrastructure.products;

import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.domain.ProductSearchCriteria;
import org.learnings.ai.shoppingassistant.services.products.ProductClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Mock stand-in for the real Go product service. The naive in-memory filtering here
 * is NOT the point — a real backend would do full-text/vector search over the catalog.
 * This exists only to return something sensible for the structured criteria while the
 * backend is wired up later.
 */
@Component
public class MockProductClient implements ProductClient {

    private static final List<Product> CATALOG = List.of(
            new Product(UUID.randomUUID(), "Awesome Widget", "gadgets", 19.99F),
            new Product(UUID.randomUUID(), "Super Gadget", "gadgets", 29.99F),
            new Product(UUID.randomUUID(), "Espresso Maker", "kitchen", 45.00F));

    @Override
    public List<Product> getAllProducts() {
        return CATALOG;
    }

    @Override
    public List<Product> search(ProductSearchCriteria criteria) {
        return CATALOG.stream()
                .filter(product -> matchesQuery(product, criteria.query()))
                .filter(product -> criteria.maxPrice() == null || product.price() <= criteria.maxPrice())
                .filter(product -> matchesCategory(product, criteria.category()))
                .toList();
    }

    private boolean matchesQuery(Product product, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        return product.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private boolean matchesCategory(Product product, String category) {
        if (category == null || category.isBlank()) {
            return true;
        }

        return product.category().equalsIgnoreCase(category);
    }
}
