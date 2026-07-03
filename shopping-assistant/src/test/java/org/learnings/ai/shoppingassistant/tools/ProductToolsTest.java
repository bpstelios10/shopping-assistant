package org.learnings.ai.shoppingassistant.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.services.products.ProductService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductToolsTest {

    private static final List<Product> EXPECTED_PRODUCTS = List.of(
            new Product(null, "Product1", 10.0F),
            new Product(null, "Awesome Item   For sure", 10.0F),
            new Product(null, "Product2", 20.0F));

    @Mock
    private ProductService productService;
    @InjectMocks
    private ProductTools productTools;

    @Test
    void listAllProducts_returnsFromService() {
        when(productService.getProductDetails()).thenReturn(EXPECTED_PRODUCTS);

        List<Product> allProducts = productTools.listAllProducts();

        assertThat(allProducts).containsExactlyInAnyOrder(EXPECTED_PRODUCTS.toArray(new Product[0]));
    }

    @Test
    void searchProducts_whenProductMatchesButDifferentCase_returnsProduct() {
        when(productService.getProductDetails()).thenReturn(EXPECTED_PRODUCTS);

        List<Product> allProducts = productTools.searchProducts("blah blah product1 blah blah");

        assertThat(allProducts).hasSize(1);
        assertThat(allProducts.getFirst().price()).isEqualTo(10F);
    }

    @Test
    void searchProducts_whenProductMatchesButPlural_returnsProduct() {
        when(productService.getProductDetails()).thenReturn(EXPECTED_PRODUCTS);

        List<Product> allProducts = productTools.searchProducts("blah blah product1s blah blah");

        assertThat(allProducts).hasSize(1);
        assertThat(allProducts.getFirst().price()).isEqualTo(10F);
    }

    @Test
    void searchProducts_whenProductShorterWord_returnsProduct() {
        when(productService.getProductDetails()).thenReturn(EXPECTED_PRODUCTS);

        List<Product> allProducts = productTools.searchProducts("blah blah prod blah blah");

        assertThat(allProducts).hasSize(2);
    }

    @Test
    void searchProducts_whenNoProductMatches_returnsEmpty() {
        when(productService.getProductDetails()).thenReturn(EXPECTED_PRODUCTS);

        List<Product> allProducts = productTools.searchProducts("blah blah prod3 blah blah");

        assertThat(allProducts).hasSize(0);
    }

    @Test
    void searchProducts_whenQueryStartsWithDelimiter_ignoresBlankTokenAndMatches() {
        when(productService.getProductDetails()).thenReturn(EXPECTED_PRODUCTS);

        // Leading non-word char makes split("\\W+") emit an empty first token,
        // exercising the !word.isBlank() filter branch.
        List<Product> allProducts = productTools.searchProducts("!product1");

        assertThat(allProducts).hasSize(1);
        assertThat(allProducts.getFirst().name()).isEqualTo("Product1");
    }

    @Test
    void searchProducts_whenQueryTermContainsProductWord_returnsProduct() {
        when(productService.getProductDetails()).thenReturn(EXPECTED_PRODUCTS);

        List<Product> allProducts = productTools.searchProducts("blah itemized blah");

        assertThat(allProducts).hasSize(1);
        assertThat(allProducts.getFirst().name()).isEqualTo("Awesome Item   For sure");
    }

    @ParameterizedTest
    @ValueSource(strings = {" "})
    @NullAndEmptySource
    void searchProducts_whenNoSearchQuery_returnsEmpty(String query) {
        List<Product> allProducts = productTools.searchProducts(query);

        assertThat(allProducts).hasSize(0);
    }
}
