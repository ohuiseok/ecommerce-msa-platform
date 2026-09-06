package com.ecommerce.monolith.payment.repository;

import com.ecommerce.monolith.payment.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {

    Optional<PaymentWebhookEvent> findByPgEventId(String pgEventId);
}
