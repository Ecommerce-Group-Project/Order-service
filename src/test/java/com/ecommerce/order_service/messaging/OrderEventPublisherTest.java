package com.ecommerce.order_service.messaging;

import com.ecommerce.order_service.config.RabbitMQConfig;
import com.ecommerce.order_service.entity.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private AmqpTemplate amqpTemplate;

    @InjectMocks
    private OrderEventPublisher orderEventPublisher;

    @Test
    @SuppressWarnings("unchecked")
    void testPublishOrderEvent() {
        // Arrange
        Order order = new Order();
        order.setOrderId(10L);
        order.setCustomerId(1L);
        order.setProductName("Test Product");
        order.setTotalPrice(200.0);

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);

        // Act
        orderEventPublisher.publishOrderEvent(order);

        // Assert
        verify(amqpTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY),
                messageCaptor.capture()
        );

        Map<String, Object> capturedMessage = messageCaptor.getValue();
        assertEquals(10L, capturedMessage.get("orderId"));
        assertEquals(1L, capturedMessage.get("customerId"));
        assertEquals("Test Product", capturedMessage.get("productName"));
        assertEquals(200.0, capturedMessage.get("totalPrice"));
    }
}
