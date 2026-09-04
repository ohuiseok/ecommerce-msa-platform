package com.ecommerce.monolith.payment.service;

import com.ecommerce.monolith.common.exception.BusinessException;
import com.ecommerce.monolith.common.exception.ErrorCode;
import com.ecommerce.monolith.order.dto.OrderResponse;
import com.ecommerce.monolith.order.entity.Order;
import com.ecommerce.monolith.order.service.OrderService;
import com.ecommerce.monolith.payment.client.MockPgClient;
import com.ecommerce.monolith.payment.dto.PaymentRequest;
import com.ecommerce.monolith.payment.dto.PaymentResponse;
import com.ecommerce.monolith.payment.entity.Payment;
import com.ecommerce.monolith.payment.entity.PaymentReconciliationTask;
import com.ecommerce.monolith.payment.repository.PaymentRepository;
import com.ecommerce.monolith.payment.repository.PaymentReconciliationTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentReconciliationTaskRepository reconciliationTaskRepository;
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

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_STATUS,
                    "결제 대기 상태의 주문만 결제할 수 있습니다: " + order.getStatus()
            );
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
            orderService.cancelPendingOrderAfterPaymentFailure(order.getOrderId());
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

    public Optional<PaymentResponse.PaymentReconciliationTaskInfo> registerLateApprovedPaymentForReconciliation(
            MockPgClient.PgEvent event
    ) {
        if (event.type() != MockPgClient.PgEventType.PAYMENT_APPROVED) {
            return Optional.empty();
        }

        OrderResponse.OrderInfo order = orderService.getOrder(event.orderId());
        if (order.getStatus() != Order.OrderStatus.CANCELLED) {
            return Optional.empty();
        }

        PaymentReconciliationTask task = reconciliationTaskRepository.findByPgEventId(event.eventId())
                .orElseGet(() -> reconciliationTaskRepository.save(PaymentReconciliationTask.builder()
                        .type(PaymentReconciliationTask.ReconciliationType.LATE_PAYMENT_APPROVED_AFTER_ORDER_CANCELLED)
                        .orderId(order.getOrderId())
                        .userId(order.getUserId())
                        .pgEventId(event.eventId())
                        .pgDeliveryId(event.deliveryId())
                        .pgTransactionId(event.transactionId())
                        .amount(event.amount())
                        .reason("취소된 주문에 늦은 결제 승인 이벤트가 도착했습니다. PG 환불 또는 수동 보정이 필요합니다.")
                        .pgOccurredAt(event.occurredAt())
                        .build()));

        log.warn("event=payment.reconciliation_task_registered orderId={} userId={} pgEventId={} pgTransactionId={} amount={} taskId={}",
                task.getOrderId(), task.getUserId(), task.getPgEventId(), task.getPgTransactionId(), task.getAmount(), task.getTaskId());

        return Optional.of(PaymentResponse.PaymentReconciliationTaskInfo.from(task));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse.PaymentReconciliationTaskInfo> getOpenPaymentReconciliationTasks() {
        return reconciliationTaskRepository
                .findByStatusOrderByCreatedAtDesc(PaymentReconciliationTask.ReconciliationStatus.OPEN)
                .stream()
                .map(PaymentResponse.PaymentReconciliationTaskInfo::from)
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
