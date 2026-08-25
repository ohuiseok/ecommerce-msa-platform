package com.ecommerce.monolith.coupon.repository;

import com.ecommerce.monolith.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    boolean existsByUserIdAndCoupon_CouponId(Long userId, Long couponId);

    List<UserCoupon> findByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE user_coupons
            SET status = 'USED',
                used_at = CURRENT_TIMESTAMP,
                order_id = :orderId
            WHERE user_coupon_id = :userCouponId
              AND status = 'ISSUED'
            """, nativeQuery = true)
    int markUsedIfIssued(@Param("userCouponId") Long userCouponId, @Param("orderId") Long orderId);
}
