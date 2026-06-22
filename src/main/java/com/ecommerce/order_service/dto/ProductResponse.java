package com.ecommerce.order_service.dto;

import lombok.Data;

@Data
public class ProductResponse {
    private Long productId;
    private String name;
    private Double unitPrice;
}