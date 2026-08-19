package com.ecommerce.monolith.coupon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupons", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "coupon_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userCouponId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CouponStatus status = CouponStatus.ISSUED;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    private LocalDateTime usedAt;

    private Long orderId;

    public enum CouponStatus {
        ISSUED, USED
    }

    public void use(Long orderId) {
        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
        this.orderId = orderId;
    }

    public void restore() {
        this.status = CouponStatus.ISSUED;
        this.usedAt = null;
        this.orderId = null;
    }
}
