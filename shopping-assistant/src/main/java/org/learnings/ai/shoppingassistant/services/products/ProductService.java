package org.learnings.ai.shoppingassistant.services.products;

import org.learnings.ai.shoppingassistant.domain.Product;

import java.util.List;

public interface ProductService {
    List<Product> getProductDetails();
}
