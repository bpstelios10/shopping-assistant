package org.learnings.ai.shoppingassistant.infrastructure.orders;

import lombok.extern.slf4j.Slf4j;
import org.learnings.ai.shoppingassistant.domain.Order;
import org.learnings.ai.shoppingassistant.services.orders.OrderClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

// TODO add exception handling
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

//                    if (status.isError()) {
//                        throw new RestClientResponseException(
//                                "Request failed with status " + status,
//                                status.value(),
//                                response.getStatusText(),
//                                response.getHeaders(),
//                                response.getBody().readAllBytes(),
//                                StandardCharsets.UTF_8
//                        );
//                    }

                    return Optional
                            .ofNullable(response.bodyTo(OrderClientResponse.class))
                            .map(OrderClientResponse::toDomain);
                });
    }
}
