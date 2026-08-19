package com.ecommerce.monolith.coupon.dto;

import com.ecommerce.monolith.coupon.entity.Coupon;
import com.ecommerce.monolith.coupon.entity.UserCoupon;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponResponse {

    @Data
    @Builder
    public static class CouponInfo {
        private Long couponId;
        private String code;
        private String name;
        private Coupon.DiscountType discountType;
        private BigDecimal discountValue;
        private BigDecimal minOrderAmount;
        private BigDecimal maxDiscountAmount;
        private LocalDateTime validFrom;
        private LocalDateTime validUntil;
        private Integer issueLimit;
        private Integer issuedCount;

        public static CouponInfo from(Coupon coupon) {
            return CouponInfo.builder()
                    .couponId(coupon.getCouponId())
                    .code(coupon.getCode())
                    .name(coupon.getName())
                    .discountType(coupon.getDiscountType())
                    .discountValue(coupon.getDiscountValue())
                    .minOrderAmount(coupon.getMinOrderAmount())
                    .maxDiscountAmount(coupon.getMaxDiscountAmount())
                    .validFrom(coupon.getValidFrom())
                    .validUntil(coupon.getValidUntil())
                    .issueLimit(coupon.getIssueLimit())
                    .issuedCount(coupon.getIssuedCount())
                    .build();
        }
    }

    @Data
    @Builder
    public static class UserCouponInfo {
        private Long userCouponId;
        private Long userId;
        private CouponInfo coupon;
        private UserCoupon.CouponStatus status;
        private LocalDateTime issuedAt;
        private LocalDateTime usedAt;
        private Long orderId;

        public static UserCouponInfo from(UserCoupon userCoupon) {
            return UserCouponInfo.builder()
                    .userCouponId(userCoupon.getUserCouponId())
                    .userId(userCoupon.getUserId())
                    .coupon(CouponInfo.from(userCoupon.getCoupon()))
                    .status(userCoupon.getStatus())
                    .issuedAt(userCoupon.getIssuedAt())
                    .usedAt(userCoupon.getUsedAt())
                    .orderId(userCoupon.getOrderId())
                    .build();
        }
    }
}
