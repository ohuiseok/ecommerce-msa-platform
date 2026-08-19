package com.ecommerce.monolith.coupon.repository;

import com.ecommerce.monolith.coupon.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Page<Coupon> findByValidUntilAfter(LocalDateTime now, Pageable pageable);

    /**
     * 발급 수량 제한을 조건부 UPDATE로 원자적으로 확인/증가시킨다.
     * 재고 차감(decreaseStockIfAvailable)과 동일한 방식으로 동시 발급 시 초과 발급을 막는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE coupons
            SET issued_count = issued_count + 1
            WHERE coupon_id = :couponId
              AND (issue_limit IS NULL OR issued_count < issue_limit)
            """, nativeQuery = true)
    int incrementIssuedCountIfAvailable(@Param("couponId") Long couponId);
}
