package com.ecommerce.monolith.review.controller;

import com.ecommerce.monolith.review.dto.ReviewRequest;
import com.ecommerce.monolith.review.dto.ReviewResponse;
import com.ecommerce.monolith.review.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Review", description = "상품 리뷰 작성/조회/수정/삭제 API")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse.ReviewInfo> createReview(
            @Valid @RequestBody ReviewRequest.Create request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(reviewService.createReview(userId, request));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse.ReviewInfo> getReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.getReview(reviewId));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse.ReviewInfo>> getReviewsByProduct(
            @PathVariable Long productId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId, pageable));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponse.ReviewInfo>> getMyReviews(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(reviewService.getMyReviews(userId));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse.ReviewInfo> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest.Update request,
            Authentication authentication) {
        ReviewResponse.ReviewInfo existing = reviewService.getReview(reviewId);
        validateSelfOrAdmin(existing.getUserId(), authentication);

        return ResponseEntity.ok(reviewService.updateReview(reviewId, request));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId, Authentication authentication) {
        ReviewResponse.ReviewInfo existing = reviewService.getReview(reviewId);
        validateSelfOrAdmin(existing.getUserId(), authentication);

        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Review API is running");
    }

    private void validateSelfOrAdmin(Long resourceUserId, Authentication authentication) {
        Long authenticatedUserId = (Long) authentication.getPrincipal();
        if (!resourceUserId.equals(authenticatedUserId) && !isAdmin(authentication)) {
            throw new AccessDeniedException("접근 권한이 없습니다");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
