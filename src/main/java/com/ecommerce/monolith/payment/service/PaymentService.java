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

import java.util.List;
import java.util.stream.Collectors;

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
        String idempotencyKey = request.getIdempotencyKey().trim();
        paymentRepository.lockIdempotencyKey(order.getOrderId() + ":" + idempotencyKey);

        return paymentRepository.findByOrderIdAndIdempotencyKey(order.getOrderId(), idempotencyKey)
                .map(payment -> {
                    log.info("event=payment.idempotent_retry orderId={} paymentId={} userId={} status={}",
                            order.getOrderId(), payment.getPaymentId(), payment.getUserId(), payment.getStatus());
                    return PaymentResponse.PaymentInfo.from(payment);
                })
                .orElseGet(() -> createPayment(request, order, idempotencyKey));
    }

    private PaymentResponse.PaymentInfo createPayment(
            PaymentRequest.Create request,
            OrderResponse.OrderInfo order,
            String idempotencyKey
    ) {
        if (paymentRepository.existsByOrderIdAndStatus(order.getOrderId(), Payment.PaymentStatus.COMPLETED)) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        Payment payment = Payment.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .amount(order.getTotalAmount())
                .method(request.getMethod())
                .idempotencyKey(idempotencyKey)
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
            log.info("event=payment.completed orderId={} paymentId={} userId={} amount={}",
                    order.getOrderId(), savedPayment.getPaymentId(), savedPayment.getUserId(), savedPayment.getAmount());
        } else {
            log.warn("event=payment.failed orderId={} paymentId={} userId={} reason={}",
                    order.getOrderId(), savedPayment.getPaymentId(), savedPayment.getUserId(), savedPayment.getFailureReason());
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

    @Transactional(readOnly = true)
    public List<PaymentResponse.PaymentOrderMismatchInfo> getPaymentOrderMismatches() {
        return paymentRepository.findPaymentOrderMismatches().stream()
                .map(PaymentResponse.PaymentOrderMismatchInfo::from)
                .collect(Collectors.toList());
    }

    public PaymentResponse.PaymentInfo cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED, "완료된 결제만 취소할 수 있습니다");
        }

        payment.cancel();
        Payment savedPayment = paymentRepository.save(payment);

        log.info("event=payment.cancelled orderId={} paymentId={} userId={}",
                savedPayment.getOrderId(), savedPayment.getPaymentId(), savedPayment.getUserId());

        return PaymentResponse.PaymentInfo.from(savedPayment);
    }
}
