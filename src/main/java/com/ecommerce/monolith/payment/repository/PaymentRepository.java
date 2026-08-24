package com.ecommerce.monolith.payment.repository;

import com.ecommerce.monolith.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByOrderIdAndIdempotencyKey(Long orderId, String idempotencyKey);

    boolean existsByOrderIdAndStatus(Long orderId, Payment.PaymentStatus status);
}
