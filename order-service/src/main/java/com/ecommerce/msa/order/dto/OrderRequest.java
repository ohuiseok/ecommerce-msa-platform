package com.ecommerce.msa.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

public class OrderRequest {

    @Data
    public static class Create {
        @NotNull(message = "사용자 ID는 필수입니다")
        private Long userId;

        @NotEmpty(message = "주문 항목은 필수입니다")
        @Valid
        private List<OrderItemRequest> orderItems;

        @NotNull(message = "배송 주소는 필수입니다")
        @Valid
        private ShippingAddressRequest shippingAddress;
    }

    @Data
    public static class OrderItemRequest {
        @NotNull(message = "상품 ID는 필수입니다")
        private Long productId;

        @NotNull(message = "수량은 필수입니다")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
        private Integer quantity;
    }

    @Data
    public static class ShippingAddressRequest {
        @NotBlank(message = "우편번호는 필수입니다")
        private String zipCode;

        @NotBlank(message = "주소는 필수입니다")
        private String address;

        private String detailAddress;

        @NotBlank(message = "수령인 이름은 필수입니다")
        private String recipientName;

        @NotBlank(message = "수령인 전화번호는 필수입니다")
        private String recipientPhone;
    }

    @Data
    public static class StatusUpdate {
        @NotNull(message = "주문 상태는 필수입니다")
        private String status;  // CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }
}
