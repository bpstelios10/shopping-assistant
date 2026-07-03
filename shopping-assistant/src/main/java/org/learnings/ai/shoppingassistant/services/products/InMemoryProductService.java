package org.learnings.ai.shoppingassistant.services.products;

import org.learnings.ai.shoppingassistant.domain.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class InMemoryProductService implements ProductService {

    @Override
    public List<Product> getProductDetails() {
        // Mock implementation: return a hardcoded product detail based on the productId
        return List.of(
                // Description: A great widget for all your needs.
                new Product(UUID.randomUUID(), "Awesome Widget", 19.99F),
                //An amazing gadget that makes life easier.
                new Product(UUID.randomUUID(), "Super Gadget", 29.99F));
    }
}
