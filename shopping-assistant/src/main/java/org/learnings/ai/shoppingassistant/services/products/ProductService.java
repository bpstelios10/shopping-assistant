package org.learnings.ai.shoppingassistant.services.products;

import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.domain.ProductSearchCriteria;

import java.util.List;

public interface ProductService {

    List<Product> getAllProducts();

    List<Product> search(ProductSearchCriteria criteria);

    List<String> getAllCategories();
}
