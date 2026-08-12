package org.learnings.ai.shoppingassistant.services.products;

import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.domain.ProductSearchCriteria;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductClient productClient;

    public ProductServiceImpl(ProductClient productClient) {
        this.productClient = productClient;
    }

    @Override
    @Cacheable("all-products")
    public List<Product> getAllProducts() {
        return productClient.getAllProducts();
    }

    @Override
    public Optional<Product> getProductById(UUID productId) {
        return productClient.getProductById(productId);
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
