package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderRequest;
import com.ecommerce.order_service.dto.ProductResponse;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.messaging.OrderEventPublisher;
import com.ecommerce.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final OrderEventPublisher eventPublisher;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${product.service.mock-fallback:true}")
    private boolean mockFallback;

    public Order createOrder(OrderRequest request) {
        // 1. Call Product Service
        ProductResponse product = fetchProduct(request.getProductId());

        if (product == null) {
            throw new RuntimeException("Product not found with ID: " + request.getProductId());
        }

        // 2. Calculate total price
        double totalPrice = product.getUnitPrice() * request.getQuantity();

        // 3. Save order
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setProductId(request.getProductId());
        order.setProductName(product.getName());
        order.setQuantity(request.getQuantity());
        order.setTotalPrice(totalPrice);

        Order saved = orderRepository.save(order);

        // 4. Publish to RabbitMQ
        eventPublisher.publishOrderEvent(saved);

        return saved;
    }

    private ProductResponse fetchProduct(Long productId) {
        String url = productServiceUrl + "/products/" + productId;
        try {
            ProductResponse product = restTemplate.getForObject(url, ProductResponse.class);
            log.info("Fetched product from Product Service: {}", product);
            return product;
        } catch (ResourceAccessException ex) {
            if (mockFallback) {
                log.warn("Product Service unavailable. Using mock product for testing. " +
                         "Set 'product.service.mock-fallback=false' to disable.");
                ProductResponse mock = new ProductResponse();
                mock.setProductId(productId);
                mock.setName("Mock Product (ID: " + productId + ")");
                mock.setUnitPrice(100.0);
                return mock;
            }
            throw new RuntimeException("Product Service is unavailable. Cannot create order.", ex);
        }
    }
}
