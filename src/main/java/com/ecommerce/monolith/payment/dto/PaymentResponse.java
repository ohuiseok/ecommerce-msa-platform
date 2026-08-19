package com.ecommerce.monolith.payment.dto;

import com.ecommerce.monolith.payment.entity.Payment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {

    @Data
    @Builder
    public static class PaymentInfo {
        private Long paymentId;
        private Long orderId;
        private Long userId;
        private BigDecimal amount;
        private Payment.PaymentMethod method;
        private Payment.PaymentStatus status;
        private String pgTransactionId;
        private String failureReason;
        private LocalDateTime approvedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static PaymentInfo from(Payment payment) {
            return PaymentInfo.builder()
                    .paymentId(payment.getPaymentId())
                    .orderId(payment.getOrderId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .method(payment.getMethod())
                    .status(payment.getStatus())
                    .pgTransactionId(payment.getPgTransactionId())
                    .failureReason(payment.getFailureReason())
                    .approvedAt(payment.getApprovedAt())
                    .createdAt(payment.getCreatedAt())
                    .updatedAt(payment.getUpdatedAt())
                    .build();
        }
    }
}
