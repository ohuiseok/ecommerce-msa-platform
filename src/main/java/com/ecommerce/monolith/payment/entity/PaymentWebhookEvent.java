package com.ecommerce.monolith.payment.entity;

import com.ecommerce.monolith.payment.client.MockPgClient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_webhook_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_webhook_events_pg_event_id",
                columnNames = "pg_event_id"
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PaymentWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long webhookEventId;

    @Column(name = "pg_event_id", nullable = false)
    private String pgEventId;

    @Column(name = "pg_delivery_id", nullable = false)
    private String pgDeliveryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private MockPgClient.PgEventType eventType;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "pg_transaction_id")
    private String pgTransactionId;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WebhookStatus status = WebhookStatus.RECEIVED;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "pg_occurred_at", nullable = false)
    private LocalDateTime pgOccurredAt;

    @Column(name = "received_at", nullable = false)
    @Builder.Default
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static PaymentWebhookEvent received(MockPgClient.PgEvent event) {
        return PaymentWebhookEvent.builder()
                .pgEventId(event.eventId())
                .pgDeliveryId(event.deliveryId())
                .eventType(event.type())
                .orderId(event.orderId())
                .pgTransactionId(event.transactionId())
                .amount(event.amount())
                .failureReason(event.failureReason())
                .pgOccurredAt(event.occurredAt())
                .build();
    }

    public void markProcessed() {
        this.status = WebhookStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    public void markIgnored(String reason) {
        this.status = WebhookStatus.IGNORED;
        this.failureReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = WebhookStatus.FAILED;
        this.failureReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public enum WebhookStatus {
        RECEIVED, PROCESSED, FAILED, IGNORED
    }
}
