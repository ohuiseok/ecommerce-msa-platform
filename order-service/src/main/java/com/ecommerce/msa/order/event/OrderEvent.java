package com.ecommerce.msa.order.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderEvent {
    private String eventType; // ORDER_CREATED, ORDER_CONFIRMED, ORDER_CANCELLED
    private Long orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private String orderStatus;
    private List<OrderItemEvent> orderItems;
    private LocalDateTime eventTime;

    @Data
    @Builder
    public static class OrderItemEvent {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
    }
}
