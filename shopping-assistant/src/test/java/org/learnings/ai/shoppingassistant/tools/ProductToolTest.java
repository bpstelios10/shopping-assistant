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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductToolTest {

    private static final List<Product> PRODUCTS = List.of(
            new Product(UUID.randomUUID(), "Espresso Maker", "kitchen", 45.0F));

    @Mock
    private ProductService productService;
    @InjectMocks
    private ProductTool productTool;

    @Test
    void listAllProducts_delegatesToService() {
        when(productService.getAllProducts()).thenReturn(PRODUCTS);

        List<Product> result = productTool.listAllProducts();

        assertThat(result).isEqualTo(PRODUCTS);
        verifyNoMoreInteractions(productService);
    }

    @Test
    void getProductById_whenExistingProduct_returnsProduct() {
        Product existingProduct = PRODUCTS.getFirst();
        when(productService.getProductById(existingProduct.id())).thenReturn(Optional.of(existingProduct));

        Optional<Product> result = productTool.getProductById(existingProduct.id());

        assertThat(result)
                .isNotEmpty()
                .hasValue(existingProduct);
        verifyNoMoreInteractions(productService);
    }

    @Test
    void getProductById_whenNonExistingProduct_returnsEmpty() {
        UUID nonExistingProductId = UUID.randomUUID();
        when(productService.getProductById(nonExistingProductId)).thenReturn(Optional.empty());

        Optional<Product> result = productTool.getProductById(nonExistingProductId);

        assertThat(result).isEmpty();
        verifyNoMoreInteractions(productService);
    }

    @Test
    void searchProducts_mapsParamsToCriteriaAndDelegates() {
        ArgumentCaptor<ProductSearchCriteria> captor = ArgumentCaptor.forClass(ProductSearchCriteria.class);
        when(productService.search(captor.capture())).thenReturn(PRODUCTS);

        List<Product> result = productTool.searchProducts("espresso maker", 50.0, "kitchen");

        assertThat(result).isEqualTo(PRODUCTS);
        assertThat(captor.getValue())
                .isEqualTo(new ProductSearchCriteria("espresso maker", 50.0, "kitchen"));
        verifyNoMoreInteractions(productService);
    }

    @Test
    void searchProducts_passesNullOptionalFilters() {
        ArgumentCaptor<ProductSearchCriteria> captor = ArgumentCaptor.forClass(ProductSearchCriteria.class);
        when(productService.search(captor.capture())).thenReturn(List.of());

        productTool.searchProducts("widget", null, null);

        assertThat(captor.getValue())
                .isEqualTo(new ProductSearchCriteria("widget", null, null));
        verifyNoMoreInteractions(productService);
    }

    @Test
    void searchProducts_whenQueryAndBlankCategoryAndNonExistingProduct_returnsEmptyList() {
        ArgumentCaptor<ProductSearchCriteria> captor = ArgumentCaptor.forClass(ProductSearchCriteria.class);
        when(productService.search(captor.capture())).thenReturn(List.of());

        productTool.searchProducts("widget", null, "");

        assertThat(captor.getValue())
                .isEqualTo(new ProductSearchCriteria("widget", null, ""));
        verifyNoMoreInteractions(productService);
    }

    @Test
    void searchProducts_whenQueryAndCategoryAndNonExistingProduct_returnsSimilarCategoryProducts() {
        ArgumentCaptor<ProductSearchCriteria> captor = ArgumentCaptor.forClass(ProductSearchCriteria.class);
        when(productService.search(captor.capture()))
                .thenReturn(List.of())
                .thenReturn(PRODUCTS);

        List<Product> result = productTool.searchProducts("widget", null, "something");

        assertThat(result).isEqualTo(PRODUCTS);
        assertThat(captor.getAllValues())
                .containsExactly(
                        new ProductSearchCriteria("widget", null, "something"),
                        new ProductSearchCriteria(null, null, "something")
                );
        verifyNoMoreInteractions(productService);
    }
}
