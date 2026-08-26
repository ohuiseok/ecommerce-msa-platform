package com.ecommerce.monolith.payment.dto;

import com.ecommerce.monolith.payment.entity.Payment;
import com.ecommerce.monolith.payment.repository.PaymentOrderMismatchProjection;
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
        private String idempotencyKey;
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
                    .idempotencyKey(payment.getIdempotencyKey())
                    .pgTransactionId(payment.getPgTransactionId())
                    .failureReason(payment.getFailureReason())
                    .approvedAt(payment.getApprovedAt())
                    .createdAt(payment.getCreatedAt())
                    .updatedAt(payment.getUpdatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    public static class PaymentOrderMismatchInfo {
        private Long orderId;
        private Long orderUserId;
        private String orderStatus;
        private Long paymentId;
        private String paymentStatus;
        private BigDecimal paymentAmount;
        private String mismatchType;
        private LocalDateTime orderUpdatedAt;
        private LocalDateTime paymentUpdatedAt;

        public static PaymentOrderMismatchInfo from(PaymentOrderMismatchProjection projection) {
            return PaymentOrderMismatchInfo.builder()
                    .orderId(projection.getOrderId())
                    .orderUserId(projection.getOrderUserId())
                    .orderStatus(projection.getOrderStatus())
                    .paymentId(projection.getPaymentId())
                    .paymentStatus(projection.getPaymentStatus())
                    .paymentAmount(projection.getPaymentAmount())
                    .mismatchType(projection.getMismatchType())
                    .orderUpdatedAt(projection.getOrderUpdatedAt())
                    .paymentUpdatedAt(projection.getPaymentUpdatedAt())
                    .build();
        }
    }
}
