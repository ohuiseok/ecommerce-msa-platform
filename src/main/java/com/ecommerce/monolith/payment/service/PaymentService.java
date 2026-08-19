package com.ecommerce.monolith.payment.service;

import com.ecommerce.monolith.common.exception.BusinessException;
import com.ecommerce.monolith.common.exception.ErrorCode;
import com.ecommerce.monolith.order.dto.OrderResponse;
import com.ecommerce.monolith.order.service.OrderService;
import com.ecommerce.monolith.payment.client.MockPgClient;
import com.ecommerce.monolith.payment.dto.PaymentRequest;
import com.ecommerce.monolith.payment.dto.PaymentResponse;
import com.ecommerce.monolith.payment.entity.Payment;
import com.ecommerce.monolith.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final MockPgClient mockPgClient;

    public PaymentResponse.PaymentInfo requestPayment(PaymentRequest.Create request) {
        OrderResponse.OrderInfo order = orderService.getOrder(request.getOrderId());

        paymentRepository.findByOrderId(order.getOrderId())
                .filter(payment -> payment.getStatus() == Payment.PaymentStatus.COMPLETED)
                .ifPresent(payment -> {
                    throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
                });

        Payment payment = Payment.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .amount(order.getTotalAmount())
                .method(request.getMethod())
                .build();

        MockPgClient.PgResult result = mockPgClient.charge(payment.getAmount(), payment.getMethod(), request.getCardNumber());
        if (result.success()) {
            payment.complete(result.transactionId());
        } else {
            payment.fail(result.failureReason());
        }

        Payment savedPayment = paymentRepository.save(payment);

        if (savedPayment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            orderService.markOrderConfirmed(order.getOrderId());
            log.info("Payment completed: orderId={}, paymentId={}", order.getOrderId(), savedPayment.getPaymentId());
        } else {
            log.warn("Payment failed: orderId={}, reason={}", order.getOrderId(), savedPayment.getFailureReason());
        }

        return PaymentResponse.PaymentInfo.from(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse.PaymentInfo getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        return PaymentResponse.PaymentInfo.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse.PaymentInfo getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        return PaymentResponse.PaymentInfo.from(payment);
    }

    public PaymentResponse.PaymentInfo cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED, "완료된 결제만 취소할 수 있습니다");
        }

        payment.cancel();
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment cancelled: paymentId={}", paymentId);

        return PaymentResponse.PaymentInfo.from(savedPayment);
    }
}
