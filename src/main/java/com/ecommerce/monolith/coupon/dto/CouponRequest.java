package com.ecommerce.monolith.coupon.dto;

import com.ecommerce.monolith.coupon.entity.Coupon;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponRequest {

    @Data
    public static class Create {
        @NotBlank(message = "쿠폰 코드는 필수입니다")
        private String code;

        @NotBlank(message = "쿠폰 이름은 필수입니다")
        private String name;

        @NotNull(message = "할인 방식은 필수입니다")
        private Coupon.DiscountType discountType;

        @NotNull(message = "할인 값은 필수입니다")
        @DecimalMin(value = "0.0", inclusive = false, message = "할인 값은 0보다 커야 합니다")
        private BigDecimal discountValue;

        private BigDecimal minOrderAmount;

        private BigDecimal maxDiscountAmount;

        @NotNull(message = "발급 시작일은 필수입니다")
        private LocalDateTime validFrom;

        @NotNull(message = "발급 종료일은 필수입니다")
        private LocalDateTime validUntil;

        @Min(value = 1, message = "발급 수량은 1 이상이어야 합니다")
        private Integer issueLimit;
    }
}
