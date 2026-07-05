package com.ecommerce.order_service.messaging;

import com.ecommerce.order_service.config.RabbitMQConfig;
import com.ecommerce.order_service.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final AmqpTemplate amqpTemplate;

    public void publishOrderEvent(Order order) {
        Map<String, Object> message = new HashMap<>();
        message.put("orderId", order.getOrderId());
        message.put("customerId", order.getCustomerId());
        message.put("productName", order.getProductName());
        message.put("totalPrice", order.getTotalPrice());
        message.put("timestamp", System.currentTimeMillis());
        message.put("quantity", order.getQuantity());
        message.put(
                "message",
                "Order #" + order.getOrderId() +
                        " has been placed successfully. " +
                        order.getQuantity() + " x " + order.getProductName() +
                        " purchased for a total price of " + order.getTotalPrice() + "."
        );


        amqpTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                message
        );
        System.out.println("Order event published for orderId: " + order.getOrderId());
    }
}
