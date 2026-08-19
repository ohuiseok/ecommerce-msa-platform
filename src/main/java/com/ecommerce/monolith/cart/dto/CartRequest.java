package com.ecommerce.monolith.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class CartRequest {

    @Data
    public static class AddItem {
        @NotNull(message = "상품 ID는 필수입니다")
        private Long productId;

        @NotNull(message = "수량은 필수입니다")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
        private Integer quantity;
    }

    @Data
    public static class UpdateItem {
        @NotNull(message = "수량은 필수입니다")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
        private Integer quantity;
    }
}
