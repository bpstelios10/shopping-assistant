package org.learnings.ai.shoppingassistant.services.products;

import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.domain.ProductSearchCriteria;

import java.util.List;

/**
 * Client to the product backend (a separate Go service, mocked for now).
 * The backend owns matching/filtering; this app only maps the shopper's request
 * into structured criteria and forwards it.
 */
public interface ProductClient {

    List<Product> getAllProducts();

    List<Product> search(ProductSearchCriteria criteria);

    List<String> getAllCategories();
}
