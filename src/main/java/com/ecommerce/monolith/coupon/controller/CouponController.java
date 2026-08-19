package com.ecommerce.monolith.coupon.controller;

import com.ecommerce.monolith.coupon.dto.CouponRequest;
import com.ecommerce.monolith.coupon.dto.CouponResponse;
import com.ecommerce.monolith.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupon", description = "쿠폰 발급 및 보유 쿠폰 조회 API")
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<CouponResponse.CouponInfo> createCoupon(@Valid @RequestBody CouponRequest.Create request) {
        return ResponseEntity.ok(couponService.createCoupon(request));
    }

    @GetMapping
    public ResponseEntity<Page<CouponResponse.CouponInfo>> getActiveCoupons(Pageable pageable) {
        return ResponseEntity.ok(couponService.getActiveCoupons(pageable));
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<CouponResponse.CouponInfo> getCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(couponService.getCoupon(couponId));
    }

    @PostMapping("/{couponId}/issue")
    public ResponseEntity<CouponResponse.UserCouponInfo> issueCoupon(
            @PathVariable Long couponId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(couponService.issueCoupon(userId, couponId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<CouponResponse.UserCouponInfo>> getMyCoupons(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(couponService.getMyCoupons(userId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Coupon API is running");
    }
}
