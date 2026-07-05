package org.learnings.ai.shoppingassistant.services.products;

import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.domain.ProductSearchCriteria;

import java.util.List;

/**
 * Application service for products. Owns orchestration/business logic and is the
 * single dependency the tools talk to. Later this is where a hybrid flow lives:
 * semantic search for candidate IDs -> client call for live price/stock -> enrichment.
 */
public interface ProductService {

    List<Product> getAllProducts();

    List<Product> search(ProductSearchCriteria criteria);

    List<String> getAllCategories();
}
