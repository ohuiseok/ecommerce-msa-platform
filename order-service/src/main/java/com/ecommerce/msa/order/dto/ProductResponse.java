package com.ecommerce.msa.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductResponse {
    private Long productId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String category;
    private String brand;
    private boolean available;

    @Data
    @Builder
    public static class StockInfo {
        private Long productId;
        private String name;
        private Integer stockQuantity;
        private boolean available;
    }
}
