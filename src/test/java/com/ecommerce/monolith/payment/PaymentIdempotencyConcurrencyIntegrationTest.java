package com.ecommerce.monolith.payment;

import com.ecommerce.monolith.order.entity.Order;
import com.ecommerce.monolith.order.entity.ShippingAddress;
import com.ecommerce.monolith.order.repository.OrderRepository;
import com.ecommerce.monolith.payment.dto.PaymentRequest;
import com.ecommerce.monolith.payment.dto.PaymentResponse;
import com.ecommerce.monolith.payment.entity.Payment;
import com.ecommerce.monolith.payment.repository.PaymentRepository;
import com.ecommerce.monolith.payment.service.PaymentService;
import com.ecommerce.monolith.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentIdempotencyConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void concurrentSameIdempotencyKeyReturnsSinglePaymentResult() throws InterruptedException {
        int concurrentRequests = 8;
        Order order = orderRepository.save(Order.builder()
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(12000))
                .shippingAddress(ShippingAddress.builder()
                        .zipCode("12345")
                        .address("Seoul")
                        .recipientName("Buyer")
                        .recipientPhone("010-0000-0000")
                        .build())
                .build());

        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        Set<Long> paymentIds = ConcurrentHashMap.newKeySet();

        CompletableFuture<?>[] tasks = new CompletableFuture[concurrentRequests];
        for (int i = 0; i < concurrentRequests; i++) {
            tasks[i] = CompletableFuture.runAsync(() -> {
                readyLatch.countDown();
                await(startLatch);

                PaymentResponse.PaymentInfo response = paymentService.requestPayment(paymentRequest(order.getOrderId()));
                paymentIds.add(response.getPaymentId());
            }, executor);
        }

        readyLatch.await();
        startLatch.countDown();
        CompletableFuture.allOf(tasks).join();
        executor.shutdown();

        List<Payment> payments = paymentRepository.findAllByOrderId(order.getOrderId());
        Order reloadedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();

        assertThat(paymentIds).hasSize(1);
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
        assertThat(payments.get(0).getIdempotencyKey()).isEqualTo("same-payment-key");
        assertThat(reloadedOrder.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
    }

    private PaymentRequest.Create paymentRequest(Long orderId) {
        PaymentRequest.Create request = new PaymentRequest.Create();
        request.setOrderId(orderId);
        request.setMethod(Payment.PaymentMethod.CARD);
        request.setIdempotencyKey("same-payment-key");
        request.setCardNumber("4111111111111112");
        return request;
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
