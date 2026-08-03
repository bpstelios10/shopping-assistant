package org.learnings.ai.shoppingassistant.services.orders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.domain.Order;
import org.learnings.ai.shoppingassistant.domain.OrderStatus;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderClient orderClient;
    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void getOrderById_whenNoOrderFound_returnsEmptyOptional() {
        UUID orderId = UUID.randomUUID();
        when(orderClient.getOrderById(orderId)).thenReturn(Optional.empty());

        Optional<Order> result = orderService.getOrderById(orderId);

        assertThat(result).isEmpty();
        verifyNoMoreInteractions(orderClient);
    }

    @Test
    void getOrderById_whenOrderIsFound_returnsOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, "some-product-id", 1, OrderStatus.CREATED);
        when(orderClient.getOrderById(orderId)).thenReturn(Optional.of(order));

        Optional<Order> result = orderService.getOrderById(orderId);

        assertThat(result)
                .isNotEmpty()
                .contains(order);
        verifyNoMoreInteractions(orderClient);
    }
}