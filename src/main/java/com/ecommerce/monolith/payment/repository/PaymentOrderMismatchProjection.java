package com.ecommerce.monolith.payment.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentOrderMismatchProjection {

    Long getOrderId();

    Long getOrderUserId();

    String getOrderStatus();

    Long getPaymentId();

    String getPaymentStatus();

    BigDecimal getPaymentAmount();

    String getMismatchType();

    LocalDateTime getOrderUpdatedAt();

    LocalDateTime getPaymentUpdatedAt();
}
