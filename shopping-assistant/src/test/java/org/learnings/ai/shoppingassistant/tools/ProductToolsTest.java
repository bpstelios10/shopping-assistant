package org.learnings.ai.shoppingassistant.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.domain.ProductSearchCriteria;
import org.learnings.ai.shoppingassistant.services.products.ProductService;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductToolsTest {

    private static final List<Product> PRODUCTS = List.of(
            new Product(UUID.randomUUID(), "Espresso Maker", "kitchen", 45.0F));

    @Mock
    private ProductService productService;
    @InjectMocks
    private ProductTools productTools;

    @Test
    void listAllProducts_delegatesToService() {
        when(productService.getAllProducts()).thenReturn(PRODUCTS);

        List<Product> result = productTools.listAllProducts();

        assertThat(result).isEqualTo(PRODUCTS);
    }

    @Test
    void searchProducts_mapsParamsToCriteriaAndDelegates() {
        ArgumentCaptor<ProductSearchCriteria> captor = ArgumentCaptor.forClass(ProductSearchCriteria.class);
        when(productService.search(captor.capture())).thenReturn(PRODUCTS);

        List<Product> result = productTools.searchProducts("espresso maker", 50.0, "kitchen");

        assertThat(result).isEqualTo(PRODUCTS);
        assertThat(captor.getValue())
                .isEqualTo(new ProductSearchCriteria("espresso maker", 50.0, "kitchen"));
    }

    @Test
    void searchProducts_passesNullOptionalFilters() {
        ArgumentCaptor<ProductSearchCriteria> captor = ArgumentCaptor.forClass(ProductSearchCriteria.class);
        when(productService.search(captor.capture())).thenReturn(List.of());

        productTools.searchProducts("widget", null, null);

        assertThat(captor.getValue())
                .isEqualTo(new ProductSearchCriteria("widget", null, null));
    }
}
