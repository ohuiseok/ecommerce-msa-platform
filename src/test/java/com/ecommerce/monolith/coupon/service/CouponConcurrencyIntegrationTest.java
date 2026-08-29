package com.ecommerce.monolith.coupon.service;

import com.ecommerce.monolith.common.exception.BusinessException;
import com.ecommerce.monolith.common.exception.ErrorCode;
import com.ecommerce.monolith.coupon.entity.Coupon;
import com.ecommerce.monolith.coupon.entity.UserCoupon;
import com.ecommerce.monolith.coupon.repository.CouponRepository;
import com.ecommerce.monolith.coupon.repository.UserCouponRepository;
import com.ecommerce.monolith.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CouponConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Test
    void concurrentMarkUsedUpdatesOnlyOneIssuedCoupon() throws InterruptedException {
        int concurrentRequests = 12;
        Coupon coupon = couponRepository.save(Coupon.builder()
                .code("CONCURRENT-USE")
                .name("동시 사용 쿠폰")
                .discountType(Coupon.DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(3000))
                .minOrderAmount(BigDecimal.ZERO)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(1))
                .build());
        UserCoupon userCoupon = userCouponRepository.save(UserCoupon.builder()
                .userId(1L)
                .coupon(coupon)
                .issuedAt(LocalDateTime.now())
                .build());

        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger alreadyUsedCount = new AtomicInteger();

        CompletableFuture<?>[] tasks = new CompletableFuture[concurrentRequests];
        for (int i = 0; i < concurrentRequests; i++) {
            long orderId = 10_000L + i;
            tasks[i] = CompletableFuture.runAsync(() -> {
                readyLatch.countDown();
                await(startLatch);

                try {
                    couponService.markUsed(userCoupon.getUserCouponId(), orderId);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.COUPON_ALREADY_USED) {
                        alreadyUsedCount.incrementAndGet();
                        return;
                    }
                    throw e;
                }
            }, executor);
        }

        readyLatch.await();
        startLatch.countDown();
        CompletableFuture.allOf(tasks).join();
        executor.shutdown();

        UserCoupon reloaded = userCouponRepository.findById(userCoupon.getUserCouponId()).orElseThrow();

        assertThat(successCount.get()).isOne();
        assertThat(alreadyUsedCount.get()).isEqualTo(concurrentRequests - 1);
        assertThat(reloaded.getStatus()).isEqualTo(UserCoupon.CouponStatus.USED);
        assertThat(reloaded.getOrderId()).isNotNull();
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
