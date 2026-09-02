package com.ecommerce.monolith.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingOrderExpirationScheduler {

    private final OrderService orderService;

    @Value("${app.order.pending-expiration.expiration-minutes:30}")
    private long expirationMinutes;

    @Value("${app.order.pending-expiration.batch-size:100}")
    private int batchSize;

    @Scheduled(
            fixedDelayString = "${app.order.pending-expiration.fixed-delay-ms:60000}",
            initialDelayString = "${app.order.pending-expiration.initial-delay-ms:60000}"
    )
    public void expirePendingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expirationMinutes);
        int expiredCount = orderService.expirePendingOrders(cutoff, batchSize);

        if (expiredCount > 0) {
            log.info("event=order.pending_expiration_completed expiredCount={} cutoff={}", expiredCount, cutoff);
        }
    }
}
