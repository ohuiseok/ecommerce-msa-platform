package com.ecommerce.monolith.coupon.service;

import com.ecommerce.monolith.common.exception.BusinessException;
import com.ecommerce.monolith.common.exception.ErrorCode;
import com.ecommerce.monolith.coupon.dto.CouponRequest;
import com.ecommerce.monolith.coupon.entity.Coupon;
import com.ecommerce.monolith.coupon.entity.UserCoupon;
import com.ecommerce.monolith.coupon.repository.CouponRepository;
import com.ecommerce.monolith.coupon.repository.UserCouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private CouponService couponService;

    @Test
    void issueCouponFailsWhenIssueLimitExhausted() {
        Coupon coupon = Coupon.builder()
                .couponId(1L)
                .code("WELCOME")
                .discountType(Coupon.DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(5000))
                .minOrderAmount(BigDecimal.ZERO)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(1))
                .issueLimit(100)
                .issuedCount(100)
                .build();

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.existsByUserIdAndCoupon_CouponId(1L, 1L)).thenReturn(false);
        when(couponRepository.incrementIssuedCountIfAvailable(1L)).thenReturn(0);

        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_ISSUE_LIMIT_EXCEEDED);
    }

    @Test
    void issueCouponFailsWhenAlreadyIssuedToUser() {
        Coupon coupon = Coupon.builder()
                .couponId(1L)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(1))
                .build();

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.existsByUserIdAndCoupon_CouponId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_ALREADY_ISSUED);
    }

    @Test
    void issueCouponFailsWhenOutsideValidPeriod() {
        Coupon coupon = Coupon.builder()
                .couponId(1L)
                .validFrom(LocalDateTime.now().minusDays(10))
                .validUntil(LocalDateTime.now().minusDays(1))
                .build();

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_IN_VALID_PERIOD);
    }

    @Test
    void issueCouponSucceedsAndPersistsUserCoupon() {
        Coupon coupon = Coupon.builder()
                .couponId(1L)
                .code("WELCOME")
                .discountType(Coupon.DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(5000))
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(1))
                .issueLimit(100)
                .issuedCount(1)
                .build();

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.existsByUserIdAndCoupon_CouponId(1L, 1L)).thenReturn(false);
        when(couponRepository.incrementIssuedCountIfAvailable(1L)).thenReturn(1);
        when(userCouponRepository.save(any(UserCoupon.class))).thenAnswer(invocation -> {
            UserCoupon userCoupon = invocation.getArgument(0);
            userCoupon.setUserCouponId(10L);
            return userCoupon;
        });

        var result = couponService.issueCoupon(1L, 1L);

        assertThat(result.getUserCouponId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(UserCoupon.CouponStatus.ISSUED);
    }

    @Test
    void calculateDiscountThrowsWhenCouponBelongsToAnotherUser() {
        Coupon coupon = Coupon.builder()
                .couponId(1L)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(1))
                .minOrderAmount(BigDecimal.ZERO)
                .build();
        UserCoupon userCoupon = UserCoupon.builder()
                .userCouponId(10L)
                .userId(2L)
                .coupon(coupon)
                .status(UserCoupon.CouponStatus.ISSUED)
                .issuedAt(LocalDateTime.now())
                .build();

        when(userCouponRepository.findById(10L)).thenReturn(Optional.of(userCoupon));

        assertThatThrownBy(() -> couponService.calculateDiscount(1L, 10L, BigDecimal.valueOf(10000)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_COUPON_NOT_FOUND);
    }

    @Test
    void calculateDiscountThrowsWhenAlreadyUsed() {
        Coupon coupon = Coupon.builder()
                .couponId(1L)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(1))
                .minOrderAmount(BigDecimal.ZERO)
                .build();
        UserCoupon userCoupon = UserCoupon.builder()
                .userCouponId(10L)
                .userId(1L)
                .coupon(coupon)
                .status(UserCoupon.CouponStatus.USED)
                .issuedAt(LocalDateTime.now())
                .build();

        when(userCouponRepository.findById(10L)).thenReturn(Optional.of(userCoupon));

        assertThatThrownBy(() -> couponService.calculateDiscount(1L, 10L, BigDecimal.valueOf(10000)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_ALREADY_USED);
    }

    @Test
    void calculateDiscountThrowsWhenMinOrderAmountNotMet() {
        Coupon coupon = Coupon.builder()
                .couponId(1L)
                .discountType(Coupon.DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(5000))
                .minOrderAmount(BigDecimal.valueOf(30000))
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(1))
                .build();
        UserCoupon userCoupon = UserCoupon.builder()
                .userCouponId(10L)
                .userId(1L)
                .coupon(coupon)
                .status(UserCoupon.CouponStatus.ISSUED)
                .issuedAt(LocalDateTime.now())
                .build();

        when(userCouponRepository.findById(10L)).thenReturn(Optional.of(userCoupon));

        assertThatThrownBy(() -> couponService.calculateDiscount(1L, 10L, BigDecimal.valueOf(10000)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
    }

    @Test
    void calculateDiscountCapsPercentageDiscountAtMaxAmount() {
        Coupon coupon = Coupon.builder()
                .couponId(1L)
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(50))
                .minOrderAmount(BigDecimal.ZERO)
                .maxDiscountAmount(BigDecimal.valueOf(3000))
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(1))
                .build();
        UserCoupon userCoupon = UserCoupon.builder()
                .userCouponId(10L)
                .userId(1L)
                .coupon(coupon)
                .status(UserCoupon.CouponStatus.ISSUED)
                .issuedAt(LocalDateTime.now())
                .build();

        when(userCouponRepository.findById(10L)).thenReturn(Optional.of(userCoupon));

        BigDecimal discount = couponService.calculateDiscount(1L, 10L, BigDecimal.valueOf(10000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(3000));
    }

    @Test
    void createCouponDefaultsMinOrderAmountToZeroWhenNotProvided() {
        CouponRequest.Create request = new CouponRequest.Create();
        request.setCode("NEWYEAR");
        request.setName("신년 쿠폰");
        request.setDiscountType(Coupon.DiscountType.FIXED_AMOUNT);
        request.setDiscountValue(BigDecimal.valueOf(1000));
        request.setValidFrom(LocalDateTime.now());
        request.setValidUntil(LocalDateTime.now().plusDays(30));

        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = couponService.createCoupon(request);

        assertThat(result.getMinOrderAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
