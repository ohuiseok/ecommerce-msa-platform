package com.ecommerce.monolith.coupon.service;

import com.ecommerce.monolith.common.exception.BusinessException;
import com.ecommerce.monolith.common.exception.ErrorCode;
import com.ecommerce.monolith.coupon.dto.CouponRequest;
import com.ecommerce.monolith.coupon.dto.CouponResponse;
import com.ecommerce.monolith.coupon.entity.Coupon;
import com.ecommerce.monolith.coupon.entity.UserCoupon;
import com.ecommerce.monolith.coupon.repository.CouponRepository;
import com.ecommerce.monolith.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public CouponResponse.CouponInfo createCoupon(CouponRequest.Create request) {
        Coupon coupon = Coupon.builder()
                .code(request.getCode())
                .name(request.getName())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : BigDecimal.ZERO)
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .issueLimit(request.getIssueLimit())
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);
        log.info("event=coupon.created couponId={} couponCode={}", savedCoupon.getCouponId(), savedCoupon.getCode());

        return CouponResponse.CouponInfo.from(savedCoupon);
    }

    @Transactional(readOnly = true)
    public CouponResponse.CouponInfo getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        return CouponResponse.CouponInfo.from(coupon);
    }

    @Transactional(readOnly = true)
    public Page<CouponResponse.CouponInfo> getActiveCoupons(Pageable pageable) {
        return couponRepository.findByValidUntilAfter(LocalDateTime.now(), pageable)
                .map(CouponResponse.CouponInfo::from);
    }

    public CouponResponse.UserCouponInfo issueCoupon(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        if (!coupon.isWithinValidPeriod(now)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_IN_VALID_PERIOD, "발급 가능한 기간이 아닙니다");
        }

        if (userCouponRepository.existsByUserIdAndCoupon_CouponId(userId, couponId)) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        int updatedRows = couponRepository.incrementIssuedCountIfAvailable(couponId);
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.COUPON_ISSUE_LIMIT_EXCEEDED);
        }

        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .issuedAt(now)
                .build();

        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);
        log.info("event=coupon.issued userId={} couponId={} userCouponId={} couponCode={}",
                userId, coupon.getCouponId(), savedUserCoupon.getUserCouponId(), coupon.getCode());

        return CouponResponse.UserCouponInfo.from(savedUserCoupon);
    }

    @Transactional(readOnly = true)
    public List<CouponResponse.UserCouponInfo> getMyCoupons(Long userId) {
        return userCouponRepository.findByUserId(userId).stream()
                .map(CouponResponse.UserCouponInfo::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(Long userId, Long userCouponId, BigDecimal orderAmount) {
        UserCoupon userCoupon = getOwnedUserCoupon(userId, userCouponId);
        Coupon coupon = userCoupon.getCoupon();

        if (userCoupon.getStatus() != UserCoupon.CouponStatus.ISSUED) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_USED);
        }
        if (!coupon.isWithinValidPeriod(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_IN_VALID_PERIOD, "사용 가능한 기간이 아닙니다");
        }
        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BusinessException(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
        }

        return coupon.calculateDiscount(orderAmount);
    }

    public void markUsed(Long userCouponId, Long orderId) {
        int updatedRows = userCouponRepository.markUsedIfIssued(userCouponId, orderId);
        if (updatedRows == 0) {
            if (!userCouponRepository.existsById(userCouponId)) {
                throw new BusinessException(ErrorCode.USER_COUPON_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.COUPON_ALREADY_USED);
        }
        log.info("event=coupon.used userCouponId={} orderId={}", userCouponId, orderId);
    }

    public void restoreCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_COUPON_NOT_FOUND));

        userCoupon.restore();
        userCouponRepository.save(userCoupon);

        log.info("event=coupon.restored userCouponId={} userId={}", userCouponId, userCoupon.getUserId());
    }

    private UserCoupon getOwnedUserCoupon(Long userId, Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_COUPON_NOT_FOUND));

        // 다른 사용자의 쿠폰인 경우 존재 여부를 노출하지 않도록 동일한 404로 응답한다
        if (!userCoupon.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.USER_COUPON_NOT_FOUND);
        }

        return userCoupon;
    }
}
