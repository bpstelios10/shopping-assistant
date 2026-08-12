package org.learnings.ai.shoppingassistant.infrastructure.products;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.domain.ProductSearchCriteria;
import org.learnings.ai.shoppingassistant.services.products.ProductClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// task: add exception handling
@Slf4j
public class RestProductClient implements ProductClient {

    private final RestClient restClient;

    public RestProductClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public @Nonnull List<Product> getAllProducts() {
        log.debug("making a request to get all products: '/products/'");

        ProductClientResponse[] response = restClient.get()
                .uri("/products")
                .retrieve()
                .body(ProductClientResponse[].class);
        log.debug("client replied with all products: [{}]", Arrays.toString(response));

        return toDomain(response);
    }

    @Override
    public Optional<Product> getProductById(UUID productId) {
        log.debug("making a get-by-id request to '/products/{id}' with id [{}]", productId);

        return restClient.get()
                .uri("/products/{id}", productId)
                .exchange((_, response) -> {
                    HttpStatusCode status = response.getStatusCode();
                    log.debug("client replied with status: [{}]", status);

                    if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
                        return Optional.empty();
                    }

                    // task: handle exceptions...

                    return Optional
                            .ofNullable(response.bodyTo(ProductClientResponse.class))
                            .map(ProductClientResponse::toDomain);
                });
    }

    @Override
    public @Nonnull List<Product> search(ProductSearchCriteria criteria) {
        log.debug("making a search request to '/products/search' with criteria [{}]", criteria);

        RestClient.ResponseSpec retrieve = restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/products/search");
                    if (criteria.query() != null) {
                        uriBuilder.queryParam("query", criteria.query());
                    }
                    if (criteria.maxPrice() != null) {
                        uriBuilder.queryParam("maxPrice", criteria.maxPrice());
                    }
                    if (criteria.category() != null) {
                        uriBuilder.queryParam("category", criteria.category());
                    }
                    return uriBuilder.build();
                })
                .retrieve();

        ProductClientResponse[] response = retrieve.body(ProductClientResponse[].class);
        log.debug("client found products: [{}]", Arrays.toString(response));

        return toDomain(response);
    }

    @Override
    public List<String> getAllCategories() {
        log.debug("making request to get all categories: '/products/categories'");

        String[] response = restClient.get()
                .uri("/products/categories")
                .retrieve()
                .body(String[].class);

        List<String> categories = response == null ? List.of() : Arrays.asList(response);
        log.debug("client replied with categories: [{}]", categories);

        return categories;
    }

    private List<Product> toDomain(ProductClientResponse[] responses) {
        if (responses == null) {
            return List.of();
        }

        return Arrays.stream(responses).map(ProductClientResponse::toDomain).toList();
    }
}
