package org.learnings.ai.shoppingassistant.services.products;

import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.domain.Product;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductServiceTest {

    private final ProductService productService = new InMemoryProductService();

    @Test
    void getProductDetails_returnsListOfProducts() {
        List<Product> products = productService.getProductDetails();

        assertThat(products).isNotEmpty().hasSize(2);
    }
}
