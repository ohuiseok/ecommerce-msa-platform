package com.ecommerce.monolith.coupon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    /** PERCENTAGE 할인일 때만 적용되는 최대 할인 한도. null이면 한도 없음. */
    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    /** 전체 발급 가능 수량. null이면 무제한. */
    private Integer issueLimit;

    @Column(nullable = false)
    @Builder.Default
    private Integer issuedCount = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DiscountType {
        FIXED_AMOUNT, PERCENTAGE
    }

    public boolean isWithinValidPeriod(LocalDateTime now) {
        return !now.isBefore(validFrom) && !now.isAfter(validUntil);
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        BigDecimal discount = switch (discountType) {
            case FIXED_AMOUNT -> discountValue;
            case PERCENTAGE -> orderAmount
                    .multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
        };

        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }
        return discount;
    }
}
