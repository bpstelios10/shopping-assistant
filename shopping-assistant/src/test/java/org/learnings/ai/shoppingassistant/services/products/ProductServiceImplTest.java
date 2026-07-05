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
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    void getAllProducts_whenProductsExist_returnsAll() {
        when(productClient.getAllProducts()).thenReturn(PRODUCTS);

        List<Product> allProducts = productService.getAllProducts();

        assertThat(allProducts).isEqualTo(PRODUCTS);
        verifyNoMoreInteractions(productClient);
    }

    @Test
    void getAllProducts_whenNoProductsExist_returnsEmpty() {
        when(productClient.getAllProducts()).thenReturn(List.of());

        List<Product> allProducts = productService.getAllProducts();

        assertThat(allProducts).isEmpty();
        verifyNoMoreInteractions(productClient);
    }

    @Test
    void search_whenProductsFound_returnsProducts() {
        ProductSearchCriteria criteria = new ProductSearchCriteria("espresso", 50.0, "kitchen");
        when(productClient.search(criteria)).thenReturn(PRODUCTS);

        List<Product> products = productService.search(criteria);

        assertThat(products).isEqualTo(PRODUCTS);
        verifyNoMoreInteractions(productClient);
    }

    @Test
    void search_whenNoProductsFound_returnsEmpty() {
        ProductSearchCriteria criteria = new ProductSearchCriteria("espresso", 50.0, "kitchen");
        when(productClient.search(criteria)).thenReturn(List.of());

        List<Product> products = productService.search(criteria);

        assertThat(products).isEmpty();
        verifyNoMoreInteractions(productClient);
    }

    @Test
    void getAllCategories_whenCategoriesExist_returnsAllCategories() {
        when(productClient.getAllCategories()).thenReturn(List.of("CLOTHES", "ACCESSORIES", "TECHNOLOGY"));

        List<String> categories = productService.getAllCategories();

        assertThat(categories).containsExactlyInAnyOrder("CLOTHES", "ACCESSORIES", "TECHNOLOGY");
        verifyNoMoreInteractions(productClient);
    }

    @Test
    void getAllCategories_whenNoCategoriesExist_returnsEmpty() {
        when(productClient.getAllCategories()).thenReturn(List.of());

        List<String> categories = productService.getAllCategories();

        assertThat(categories).isEmpty();
        verifyNoMoreInteractions(productClient);
    }
}
