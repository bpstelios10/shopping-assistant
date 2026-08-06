package org.learnings.ai.shoppingassistant.infrastructure.orders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.domain.Order;
import org.learnings.ai.shoppingassistant.domain.OrderStatus;
import org.learnings.ai.shoppingassistant.services.orders.OrderClient;
import org.learnings.ai.shoppingassistant.services.orders.OrderServiceImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestOrderClientTest {

    private MockRestServiceServer server;
    private OrderClient orderClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://orders");
        server = MockRestServiceServer.bindTo(builder).build();
        orderClient = new RestOrderClient(builder.build());
    }

    @Test
    void getOrderById_whenClientFindsOrder_returnsOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, "some-product-id", 1, OrderStatus.CREATED);
        String responseBody = """
                {
                  "id": "%s",
                  "product_id": "%s",
                  "quantity": %d,
                  "status": "%s"
                }
                """.formatted(order.id(), order.productId(), order.quantity(), order.status());

        server.expect(requestTo("http://orders/orders/" + orderId))
                .andExpect(method(GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        Optional<Order> response = orderClient.getOrderById(orderId);

        assertThat(response).isPresent()
                .get()
                .satisfies(o -> {
                    assertThat(o.id()).isEqualTo(order.id());
                    assertThat(o.productId()).isEqualTo(order.productId());
                    assertThat(o.quantity()).isEqualTo(order.quantity());
                    assertThat(o.status()).isEqualTo(order.status());
                });
        server.verify();
    }

    @Test
    void getOrderById_whenClientReturnsNotFound_returnsEmpty() {
        UUID orderId = UUID.randomUUID();
        String responseBody = """
                {
                  "Code": "ORDER_NOT_FOUND",
                  "Message": "Some message"
                }
                """;

        server.expect(requestTo("http://orders/orders/" + orderId))
                .andExpect(method(GET))
                .andRespond(withResourceNotFound().body(responseBody));

        Optional<Order> response = orderClient.getOrderById(orderId);

        assertThat(response).isEmpty();
        server.verify();
    }

    @Test
    void createOrder_succeeds() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, "some-product-id", 1, OrderStatus.CREATED);
        String responseBody = """
                {
                  "id": "%s",
                  "product_id": "%s",
                  "quantity": %d,
                  "status": "%s"
                }
                """.formatted(order.id(), order.productId(), order.quantity(), order.status());

        String requestBody = """
                {
                  "product_id": "%s",
                  "quantity": %d,
                }
                """.formatted(order.productId(), order.quantity());

        server.expect(requestTo("http://orders/orders"))
                .andExpect(method(POST))
                .andExpect(content().json(requestBody))
                .andExpect(header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.LOCATION, "/orders/" + order.id())
                        .body(responseBody));

        OrderServiceImpl.CreateOrderRequest request = new OrderServiceImpl.CreateOrderRequest(order.productId(), order.quantity());
        Order response = orderClient.createOrder(request);

        server.verify();
        assertThat(response).isEqualTo(order);
    }

    @Test
    void createOrder_whenResponseIsEmpty_throws() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, "some-product-id", 1, OrderStatus.CREATED);

        String requestBody = """
                {
                  "product_id": "%s",
                  "quantity": %d,
                }
                """.formatted(order.productId(), order.quantity());

        server.expect(requestTo("http://orders/orders"))
                .andExpect(method(POST))
                .andExpect(content().json(requestBody))
                .andExpect(header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withStatus(HttpStatus.CREATED));

        OrderServiceImpl.CreateOrderRequest request = new OrderServiceImpl.CreateOrderRequest(order.productId(), order.quantity());
        assertThatThrownBy(() -> orderClient.createOrder(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create order, response body is null");

        server.verify();
    }
}
