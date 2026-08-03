package org.learnings.ai.shoppingassistant.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.domain.Order;
import org.learnings.ai.shoppingassistant.domain.OrderStatus;
import org.learnings.ai.shoppingassistant.services.orders.OrderService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderToolTest {

    @Mock
    private OrderService orderService;
    @InjectMocks
    private OrderTool orderTool;

    @Test
    void getOrderById_whenOrderIsNotFound_returnEmpty() {
        UUID orderId = UUID.randomUUID();
        when(orderService.getOrderById(orderId)).thenReturn(Optional.empty());

        Optional<Order> result = orderTool.getOrderById(orderId);

        assertThat(result).isEmpty();
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void getOrderById_whenOrderIsFound_returnOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, "some-product-id", 1, OrderStatus.CREATED);
        when(orderService.getOrderById(orderId)).thenReturn(Optional.of(order));

        Optional<Order> result = orderTool.getOrderById(orderId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(order);
        verifyNoMoreInteractions(orderService);
    }
}