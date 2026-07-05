package org.learnings.ai.shoppingassistant.infrastructure.products;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.domain.ProductSearchCriteria;
import org.learnings.ai.shoppingassistant.services.products.ProductClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestProductClientTest {

    private MockRestServiceServer server;
    private ProductClient productClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://products");
        server = MockRestServiceServer.bindTo(builder).build();
        productClient = new RestProductClient(builder.build());
    }

    @Test
    void getAllProducts_whenProductServiceReturnsProducts_returnsThem() {
        server.expect(requestTo("http://products/products"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        [{"id":"11111111-1111-1111-1111-111111111111","name":"Espresso Maker","category":"kitchen","price":45.0}]
                        """, MediaType.APPLICATION_JSON));

        List<Product> products = productClient.getAllProducts();

        assertThat(products).singleElement()
                .satisfies(product -> {
                    assertThat(product.name()).isEqualTo("Espresso Maker");
                    assertThat(product.category()).isEqualTo("kitchen");
                    assertThat(product.price()).isEqualTo(45.0F);
                });
        server.verify();
    }

    @Test
    void getAllProducts_whenProductHasNoId_mapsIdToNull() {
        server.expect(requestTo("http://products/products"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        [{"name":"Espresso Maker","category":"kitchen","price":45.0}]
                        """, MediaType.APPLICATION_JSON));

        List<Product> products = productClient.getAllProducts();

        assertThat(products).singleElement()
                .satisfies(product -> assertThat(product.id()).isNull());
        server.verify();
    }

    @Test
    void getAllProducts_whenEmptyBody_returnsEmptyList() {
        server.expect(requestTo("http://products/products"))
                .andExpect(method(GET))
                .andRespond(withSuccess());

        List<Product> products = productClient.getAllProducts();

        assertThat(products).isEmpty();
        server.verify();
    }

    @Test
    void search_whenQueryParamsUsed_sendsThemAllToClient() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://products/products/search")))
                .andExpect(method(GET))
                .andExpect(queryParam("query", "espresso"))
                .andExpect(queryParam("maxPrice", "50.0"))
                .andExpect(queryParam("category", "kitchen"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<Product> products = productClient.search(new ProductSearchCriteria("espresso", 50.0, "kitchen"));

        assertThat(products).isEmpty();
        server.verify();
    }

    @Test
    void search_whenQueryNull_sendsOnlyOtherFilters() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://products/products/search")))
                .andExpect(method(GET))
                .andExpect(queryParam("maxPrice", "20.0"))
                .andExpect(queryParam("category", "kitchen"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<Product> products = productClient.search(new ProductSearchCriteria(null, 20.0, "kitchen"));

        assertThat(products).isEmpty();
        server.verify();
    }

    @Test
    void search_whenFiltersNull_omitsThem() {
        server.expect(requestToUriTemplate("http://products/products/search?query={q}", "widget"))
                .andExpect(method(GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<Product> products = productClient.search(new ProductSearchCriteria("widget", null, null));

        assertThat(products).isEmpty();
        server.verify();
    }

    @Test
    void getAllCategories_whenCategoriesExist_returnsCategories() {
        server.expect(requestTo("http://products/products/categories"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        ["CLOTHES", "ACCESSORIES", "TECHNOLOGY"]
                        """, MediaType.APPLICATION_JSON));

        List<String> productCategories = productClient.getAllCategories();

        assertThat(productCategories).containsExactlyInAnyOrder("CLOTHES", "ACCESSORIES", "TECHNOLOGY");
        server.verify();
    }

    @Test
    void getAllCategories_whenNoCategoriesExist_returnsEmpty() {
        server.expect(requestTo("http://products/products/categories"))
                .andExpect(method(GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<String> productCategories = productClient.getAllCategories();

        assertThat(productCategories).isEmpty();
        server.verify();
    }

    @Test
    void getAllCategories_whenEmptyBody_returnsEmpty() {
        server.expect(requestTo("http://products/products/categories"))
                .andExpect(method(GET))
                .andRespond(withSuccess());

        List<String> productCategories = productClient.getAllCategories();

        assertThat(productCategories).isEmpty();
        server.verify();
    }
}
