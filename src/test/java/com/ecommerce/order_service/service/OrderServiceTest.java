package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderRequest;
import com.ecommerce.order_service.dto.ProductResponse;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.messaging.OrderEventPublisher;
import com.ecommerce.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "productServiceUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(orderService, "mockFallback", true);
    }

    @Test
    void testCreateOrder_Success() {
        // Arrange
        OrderRequest request = new OrderRequest();
        request.setCustomerId(1L);
        request.setProductId(100L);
        request.setQuantity(2);

        ProductResponse productResponse = new ProductResponse();
        productResponse.setProductId(100L);
        productResponse.setName("Test Product");
        productResponse.setUnitPrice(50.0);

        Order savedOrder = new Order();
        savedOrder.setOrderId(10L);
        savedOrder.setCustomerId(1L);
        savedOrder.setProductId(100L);
        savedOrder.setProductName("Test Product");
        savedOrder.setQuantity(2);
        savedOrder.setTotalPrice(100.0);

        when(restTemplate.getForObject(anyString(), eq(ProductResponse.class))).thenReturn(productResponse);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        Order result = orderService.createOrder(request);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getOrderId());
        assertEquals("Test Product", result.getProductName());
        assertEquals(100.0, result.getTotalPrice());

        verify(restTemplate).getForObject("http://localhost:8081/api/products/100", ProductResponse.class);
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishOrderEvent(savedOrder);
    }

    @Test
    void testCreateOrder_ProductNotFound() {
        // Arrange
        OrderRequest request = new OrderRequest();
        request.setCustomerId(1L);
        request.setProductId(100L);
        request.setQuantity(2);

        when(restTemplate.getForObject(anyString(), eq(ProductResponse.class))).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderService.createOrder(request));
        assertTrue(exception.getMessage().contains("Product not found"));

        verify(orderRepository, never()).save(any(Order.class));
        verify(eventPublisher, never()).publishOrderEvent(any(Order.class));
    }

    @Test
    void testCreateOrder_ProductServiceDown_WithMockFallback() {
        // Arrange
        OrderRequest request = new OrderRequest();
        request.setCustomerId(1L);
        request.setProductId(100L);
        request.setQuantity(3);

        Order savedOrder = new Order();
        savedOrder.setOrderId(10L);
        savedOrder.setCustomerId(1L);
        savedOrder.setProductId(100L);
        savedOrder.setProductName("Mock Product (ID: 100)");
        savedOrder.setQuantity(3);
        savedOrder.setTotalPrice(300.0);

        when(restTemplate.getForObject(anyString(), eq(ProductResponse.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        Order result = orderService.createOrder(request);

        // Assert
        assertNotNull(result);
        assertEquals("Mock Product (ID: 100)", result.getProductName());
        assertEquals(300.0, result.getTotalPrice());

        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishOrderEvent(savedOrder);
    }

    @Test
    void testCreateOrder_ProductServiceDown_WithoutMockFallback() {
        // Arrange
        ReflectionTestUtils.setField(orderService, "mockFallback", false);

        OrderRequest request = new OrderRequest();
        request.setCustomerId(1L);
        request.setProductId(100L);
        request.setQuantity(3);

        when(restTemplate.getForObject(anyString(), eq(ProductResponse.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderService.createOrder(request));
        assertTrue(exception.getMessage().contains("Product Service is unavailable"));

        verify(orderRepository, never()).save(any(Order.class));
        verify(eventPublisher, never()).publishOrderEvent(any(Order.class));
    }
}
