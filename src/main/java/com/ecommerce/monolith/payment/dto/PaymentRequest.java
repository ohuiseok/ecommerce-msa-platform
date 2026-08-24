package com.ecommerce.monolith.payment.dto;

import com.ecommerce.monolith.payment.entity.Payment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class PaymentRequest {

    @Data
    public static class Create {
        @NotNull(message = "주문 ID는 필수입니다")
        private Long orderId;

        @NotNull(message = "결제 수단은 필수입니다")
        private Payment.PaymentMethod method;

        @NotBlank(message = "멱등키는 필수입니다")
        @Size(max = 100, message = "멱등키는 100자 이하여야 합니다")
        private String idempotencyKey;

        /** CARD 결제일 때만 사용되는 모의 카드 번호. 마지막 자리가 홀수면 결제가 거절됩니다. */
        private String cardNumber;
    }
}
