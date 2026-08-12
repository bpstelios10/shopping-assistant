package org.learnings.ai.shoppingassistant.infrastructure.orders;

import lombok.extern.slf4j.Slf4j;
import org.learnings.ai.shoppingassistant.domain.Order;
import org.learnings.ai.shoppingassistant.services.orders.OrderClient;
import org.learnings.ai.shoppingassistant.services.orders.OrderServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

// task: add exception handling
@Slf4j
public class RestOrderClient implements OrderClient {

    private final RestClient restClient;

    public RestOrderClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<Order> getOrderById(UUID orderId) {
        log.debug("making a request to get order by ID: '/orders/{}'", orderId);

        return restClient.get()
                .uri("/orders/{id}", orderId)
                .exchange((_, response) -> {
                    HttpStatusCode status = response.getStatusCode();
                    log.debug("client replied with status: [{}]", status);

                    if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
                        return Optional.empty();
                    }

                    // task: handle exceptions. use JsonProperty required as well

                    return Optional
                            .ofNullable(response.bodyTo(OrderClientResponse.class))
                            .map(OrderClientResponse::toDomain);
                });
    }

    @Override
    public Order createOrder(OrderServiceImpl.CreateOrderRequest request) {
        log.debug("making a request to create order with body: [{}]", request);

        return restClient.post()
                .uri("/orders")
                .body(request)
                .exchange((_, response) -> {
                    HttpStatusCode status = response.getStatusCode();
                    log.debug("client replied with status: [{}]", status);

                    return Optional.ofNullable(response.bodyTo(OrderClientResponse.class))
                            .map(OrderClientResponse::toDomain)
                            .orElseThrow(() -> new RuntimeException("Failed to create order, response body is null"));
                });
    }
}
