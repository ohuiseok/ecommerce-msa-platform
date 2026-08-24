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
import com.ecommerce.monolith.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private MockPgClient mockPgClient;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void requestPaymentConfirmsOrderWhenChargeSucceeds() {
        PaymentRequest.Create request = new PaymentRequest.Create();
        request.setOrderId(1L);
        request.setMethod(Payment.PaymentMethod.CARD);
        request.setCardNumber("1234567890123452");
        request.setIdempotencyKey(" pay-key-1 ");

        when(orderService.getOrder(1L)).thenReturn(OrderResponse.OrderInfo.builder()
                .orderId(1L)
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(2000))
                .status(Order.OrderStatus.PENDING)
                .build());
        when(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "pay-key-1")).thenReturn(Optional.empty());
        when(paymentRepository.existsByOrderIdAndStatus(1L, Payment.PaymentStatus.COMPLETED)).thenReturn(false);
        when(mockPgClient.charge(BigDecimal.valueOf(2000), Payment.PaymentMethod.CARD, "1234567890123452"))
                .thenReturn(MockPgClient.PgResult.success("MOCK-TX-1"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setPaymentId(100L);
            return payment;
        });

        PaymentResponse.PaymentInfo result = paymentService.requestPayment(request);

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
        assertThat(result.getPgTransactionId()).isEqualTo("MOCK-TX-1");
        assertThat(result.getIdempotencyKey()).isEqualTo("pay-key-1");
        verify(orderService).markOrderConfirmed(1L);
    }

    @Test
    void requestPaymentKeepsOrderPendingWhenChargeFails() {
        PaymentRequest.Create request = new PaymentRequest.Create();
        request.setOrderId(1L);
        request.setMethod(Payment.PaymentMethod.CARD);
        request.setCardNumber("1234567890123451");
        request.setIdempotencyKey("pay-key-2");

        when(orderService.getOrder(1L)).thenReturn(OrderResponse.OrderInfo.builder()
                .orderId(1L)
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(2000))
                .status(Order.OrderStatus.PENDING)
                .build());
        when(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "pay-key-2")).thenReturn(Optional.empty());
        when(paymentRepository.existsByOrderIdAndStatus(1L, Payment.PaymentStatus.COMPLETED)).thenReturn(false);
        when(mockPgClient.charge(BigDecimal.valueOf(2000), Payment.PaymentMethod.CARD, "1234567890123451"))
                .thenReturn(MockPgClient.PgResult.failure("카드 승인이 거절되었습니다"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse.PaymentInfo result = paymentService.requestPayment(request);

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("카드 승인이 거절되었습니다");
        verify(orderService, never()).markOrderConfirmed(any());
    }

    @Test
    void requestPaymentThrowsWhenAlreadyCompleted() {
        PaymentRequest.Create request = new PaymentRequest.Create();
        request.setOrderId(1L);
        request.setMethod(Payment.PaymentMethod.CARD);
        request.setIdempotencyKey("pay-key-3");

        Payment existing = Payment.builder()
                .paymentId(1L)
                .orderId(1L)
                .userId(1L)
                .amount(BigDecimal.valueOf(2000))
                .method(Payment.PaymentMethod.CARD)
                .status(Payment.PaymentStatus.COMPLETED)
                .build();

        when(orderService.getOrder(1L)).thenReturn(OrderResponse.OrderInfo.builder()
                .orderId(1L)
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(2000))
                .status(Order.OrderStatus.CONFIRMED)
                .build());
        when(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "pay-key-3")).thenReturn(Optional.empty());
        when(paymentRepository.existsByOrderIdAndStatus(1L, Payment.PaymentStatus.COMPLETED)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.requestPayment(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_ALREADY_PROCESSED);
    }

    @Test
    void requestPaymentReturnsExistingPaymentForSameIdempotencyKeyWithoutPgCall() {
        PaymentRequest.Create request = new PaymentRequest.Create();
        request.setOrderId(1L);
        request.setMethod(Payment.PaymentMethod.CARD);
        request.setCardNumber("1234567890123452");
        request.setIdempotencyKey("pay-key-4");

        Payment existing = Payment.builder()
                .paymentId(100L)
                .orderId(1L)
                .userId(1L)
                .amount(BigDecimal.valueOf(2000))
                .method(Payment.PaymentMethod.CARD)
                .status(Payment.PaymentStatus.FAILED)
                .idempotencyKey("pay-key-4")
                .failureReason("카드 승인이 거절되었습니다")
                .build();

        when(orderService.getOrder(1L)).thenReturn(OrderResponse.OrderInfo.builder()
                .orderId(1L)
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(2000))
                .status(Order.OrderStatus.PENDING)
                .build());
        when(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "pay-key-4")).thenReturn(Optional.of(existing));

        PaymentResponse.PaymentInfo result = paymentService.requestPayment(request);

        assertThat(result.getPaymentId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
        assertThat(result.getIdempotencyKey()).isEqualTo("pay-key-4");
        verify(mockPgClient, never()).charge(any(), any(), any());
        verify(paymentRepository, never()).save(any());
        verify(orderService, never()).markOrderConfirmed(any());
    }

    @Test
    void paymentResponseIncludesNullableIdempotencyKey() {
        Payment payment = Payment.builder()
                .paymentId(1L)
                .orderId(1L)
                .userId(1L)
                .amount(BigDecimal.valueOf(2000))
                .method(Payment.PaymentMethod.CARD)
                .status(Payment.PaymentStatus.PENDING)
                .idempotencyKey("pay-key-1")
                .build();

        PaymentResponse.PaymentInfo result = PaymentResponse.PaymentInfo.from(payment);

        assertThat(result.getIdempotencyKey()).isEqualTo("pay-key-1");
    }
}
