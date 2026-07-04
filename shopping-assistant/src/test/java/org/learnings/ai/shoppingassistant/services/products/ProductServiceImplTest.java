package org.learnings.ai.shoppingassistant.services.products;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.domain.ProductSearchCriteria;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    private static final List<Product> PRODUCTS = List.of(
            new Product(UUID.randomUUID(), "Espresso Maker", "kitchen", 45.0F));

    @Mock
    private ProductClient productClient;
    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void getAllProducts_delegatesToClient() {
        when(productClient.getAllProducts()).thenReturn(PRODUCTS);

        assertThat(productService.getAllProducts()).isEqualTo(PRODUCTS);
    }

    @Test
    void search_delegatesToClient() {
        ProductSearchCriteria criteria = new ProductSearchCriteria("espresso", 50.0, "kitchen");
        when(productClient.search(criteria)).thenReturn(PRODUCTS);

        assertThat(productService.search(criteria)).isEqualTo(PRODUCTS);
    }
}
