package org.learnings.ai.shoppingassistant.services.products;

import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.domain.ProductSearchCriteria;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductClient productClient;

    public ProductServiceImpl(ProductClient productClient) {
        this.productClient = productClient;
    }

    @Override
    public List<Product> getAllProducts() {
        return productClient.getAllProducts();
    }

    @Override
    @Cacheable(
            value = "products-per-category",
            key = "#criteria.category",
            condition = "#criteria.category != null && #criteria.query == null && #criteria.maxPrice == null"
    )
    public List<Product> search(ProductSearchCriteria criteria) {
        return productClient.search(criteria);
    }

    @Override
    @Cacheable("product-categories")
    public List<String> getAllCategories() {
        return productClient.getAllCategories();
    }
}
