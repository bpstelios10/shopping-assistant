package org.learnings.ai.shoppingassistant.infrastructure.products;

import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.domain.ProductSearchCriteria;
import org.learnings.ai.shoppingassistant.services.products.ProductClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockProductClientTest {

    private final ProductClient productClient = new MockProductClient();

    @Test
    void getAllProducts_returnsFullCatalog() {
        List<Product> products = productClient.getAllProducts();

        assertThat(products).hasSize(3);
    }

    @Test
    void search_whenNoFilters_returnsEverything() {
        List<Product> products = productClient.search(new ProductSearchCriteria(null, null, null));

        assertThat(products).hasSize(3);
    }

    @Test
    void search_whenBlankQuery_ignoresQueryFilter() {
        List<Product> products = productClient.search(new ProductSearchCriteria("  ", null, null));

        assertThat(products).hasSize(3);
    }

    @Test
    void search_whenQueryMatchesNameCaseInsensitively_returnsProduct() {
        List<Product> products = productClient.search(new ProductSearchCriteria("espresso", null, null));

        assertThat(products).singleElement()
                .extracting(Product::name).isEqualTo("Espresso Maker");
    }

    @Test
    void search_whenMaxPrice_filtersOutExpensive() {
        List<Product> products = productClient.search(new ProductSearchCriteria(null, 20.0, null));

        assertThat(products).extracting(Product::name)
                .containsExactly("Awesome Widget");
    }

    @Test
    void search_whenCategory_filtersByCategoryCaseInsensitively() {
        List<Product> products = productClient.search(new ProductSearchCriteria(null, null, "KITCHEN"));

        assertThat(products).extracting(Product::name)
                .containsExactly("Espresso Maker");
    }

    @Test
    void search_whenBlankCategory_ignoresCategoryFilter() {
        List<Product> products = productClient.search(new ProductSearchCriteria(null, null, " "));

        assertThat(products).hasSize(3);
    }

    @Test
    void search_whenCombinedFilters_appliesAll() {
        List<Product> products = productClient.search(new ProductSearchCriteria("widget", 25.0, "gadgets"));

        assertThat(products).extracting(Product::name)
                .containsExactly("Awesome Widget");
    }

    @Test
    void search_whenNothingMatches_returnsEmpty() {
        List<Product> products = productClient.search(new ProductSearchCriteria("nonexistent", null, null));

        assertThat(products).isEmpty();
    }
}
