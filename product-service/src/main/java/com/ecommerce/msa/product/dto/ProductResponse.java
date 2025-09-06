package com.ecommerce.msa.product.dto;

import com.ecommerce.msa.product.entity.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductResponse {

    @Data
    @Builder
    public static class ProductInfo {
        private Long productId;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer stockQuantity;
        private String category;
        private String brand;
        private String imageUrl;
        private Product.ProductStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static ProductInfo from(Product product) {
            return ProductInfo.builder()
                    .productId(product.getProductId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .stockQuantity(product.getStockQuantity())
                    .category(product.getCategory())
                    .brand(product.getBrand())
                    .imageUrl(product.getImageUrl())
                    .status(product.getStatus())
                    .createdAt(product.getCreatedAt())
                    .updatedAt(product.getUpdatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    public static class StockInfo {
        private Long productId;
        private String name;
        private Integer stockQuantity;
        private Product.ProductStatus status;
        private boolean available;

        public static StockInfo from(Product product) {
            return StockInfo.builder()
                    .productId(product.getProductId())
                    .name(product.getName())
                    .stockQuantity(product.getStockQuantity())
                    .status(product.getStatus())
                    .available(product.getStockQuantity() > 0 && product.getStatus() == Product.ProductStatus.ACTIVE)
                    .build();
        }
    }
}
