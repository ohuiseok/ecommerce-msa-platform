package com.ecommerce.monolith.payment.client;

import com.ecommerce.monolith.payment.entity.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 실제 PG사 연동 대신 사용하는 모의 결제 클라이언트.
 * 카드 결제는 카드번호 마지막 자리가 홀수이면 승인 거절을 재현해 실패 흐름을 테스트할 수 있게 한다.
 */
@Component
public class MockPgClient {

    public PgResult charge(BigDecimal amount, Payment.PaymentMethod method, String cardNumber) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return PgResult.failure("결제 금액이 유효하지 않습니다");
        }

        if (method == Payment.PaymentMethod.CARD && cardNumber != null && !cardNumber.isBlank()) {
            char lastDigit = cardNumber.charAt(cardNumber.length() - 1);
            if (Character.isDigit(lastDigit) && (lastDigit - '0') % 2 != 0) {
                return PgResult.failure("카드 승인이 거절되었습니다");
            }
        }

        return PgResult.success("MOCK-" + UUID.randomUUID());
    }

    public record PgResult(boolean success, String transactionId, String failureReason) {
        public static PgResult success(String transactionId) {
            return new PgResult(true, transactionId, null);
        }

        public static PgResult failure(String reason) {
            return new PgResult(false, null, reason);
        }
    }
}
