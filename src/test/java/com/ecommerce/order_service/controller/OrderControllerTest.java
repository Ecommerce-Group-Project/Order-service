package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.OrderRequest;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateOrder_Success() throws Exception {
        // Arrange
        OrderRequest request = new OrderRequest();
        request.setCustomerId(1L);
        request.setProductId(100L);
        request.setQuantity(2);

        Order order = new Order();
        order.setOrderId(10L);
        order.setCustomerId(1L);
        order.setProductId(100L);
        order.setQuantity(2);
        order.setTotalPrice(200.0);
        order.setStatus("CREATED");

        when(orderService.createOrder(any(OrderRequest.class))).thenReturn(order);

        // Act & Assert
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(10L))
                .andExpect(jsonPath("$.customerId").value(1L))
                .andExpect(jsonPath("$.productId").value(100L))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.totalPrice").value(200.0))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void testCreateOrder_InvalidRequest_MissingCustomerId() throws Exception {
        // Arrange - customer ID is null
        OrderRequest request = new OrderRequest();
        request.setProductId(100L);
        request.setQuantity(2);

        // Act & Assert
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_InvalidRequest_InvalidQuantity() throws Exception {
        // Arrange - quantity is less than 1
        OrderRequest request = new OrderRequest();
        request.setCustomerId(1L);
        request.setProductId(100L);
        request.setQuantity(0);

        // Act & Assert
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
