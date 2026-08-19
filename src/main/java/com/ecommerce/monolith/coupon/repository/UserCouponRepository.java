package com.ecommerce.monolith.coupon.repository;

import com.ecommerce.monolith.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    boolean existsByUserIdAndCoupon_CouponId(Long userId, Long couponId);

    List<UserCoupon> findByUserId(Long userId);
}
