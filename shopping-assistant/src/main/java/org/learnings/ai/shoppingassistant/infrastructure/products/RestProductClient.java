package org.learnings.ai.shoppingassistant.infrastructure.products;

import lombok.extern.slf4j.Slf4j;
import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.domain.ProductSearchCriteria;
import org.learnings.ai.shoppingassistant.services.products.ProductClient;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class RestProductClient implements ProductClient {

    private final RestClient restClient;

    public RestProductClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<Product> getAllProducts() {
        log.debug("making a request to get all products: '/products/'");

        return toDomain(restClient.get()
                .uri("/products")
                .retrieve()
                .body(ProductClientResponse[].class));
    }

    @Override
    public List<Product> search(ProductSearchCriteria criteria) {
        log.debug("making a search request to '/products/search' with criteria [{}]", criteria);

        return toDomain(restClient.get()
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
                .retrieve()
                .body(ProductClientResponse[].class));
    }

    private List<Product> toDomain(ProductClientResponse[] responses) {
        if (responses == null) {
            return List.of();
        }

        return Arrays.stream(responses).map(ProductClientResponse::toDomain).toList();
    }
}
