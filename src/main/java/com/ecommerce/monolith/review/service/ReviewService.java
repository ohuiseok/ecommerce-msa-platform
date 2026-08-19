package com.ecommerce.monolith.review.service;

import com.ecommerce.monolith.common.exception.BusinessException;
import com.ecommerce.monolith.common.exception.ErrorCode;
import com.ecommerce.monolith.order.dto.OrderResponse;
import com.ecommerce.monolith.order.entity.Order;
import com.ecommerce.monolith.order.service.OrderService;
import com.ecommerce.monolith.product.dto.ProductResponse;
import com.ecommerce.monolith.product.service.ProductService;
import com.ecommerce.monolith.review.dto.ReviewRequest;
import com.ecommerce.monolith.review.dto.ReviewResponse;
import com.ecommerce.monolith.review.entity.Review;
import com.ecommerce.monolith.review.repository.ReviewRepository;
import com.ecommerce.monolith.user.dto.UserResponse;
import com.ecommerce.monolith.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductService productService;
    private final OrderService orderService;
    private final UserService userService;

    public ReviewResponse.ReviewInfo createReview(Long userId, ReviewRequest.Create request) {
        ProductResponse.ProductInfo product = productService.getProduct(request.getProductId());
        validatePurchase(userId, request.getOrderId(), request.getProductId());

        if (reviewRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        UserResponse.UserInfo userInfo = userService.getUserById(userId);

        Review review = Review.builder()
                .userId(userId)
                .reviewerName(userInfo.getName())
                .productId(product.getProductId())
                .orderId(request.getOrderId())
                .rating(request.getRating())
                .content(request.getContent())
                .build();

        Review savedReview = reviewRepository.save(review);
        syncProductRating(product.getProductId());

        log.info("Review created: userId={}, productId={}, rating={}", userId, product.getProductId(), request.getRating());

        return ReviewResponse.ReviewInfo.from(savedReview);
    }

    private void validatePurchase(Long userId, Long orderId, Long productId) {
        OrderResponse.OrderInfo order = orderService.getOrder(orderId);

        boolean ownedByUser = order.getUserId().equals(userId);
        boolean notCancelled = order.getStatus() != Order.OrderStatus.CANCELLED;
        boolean containsProduct = order.getOrderItems().stream()
                .anyMatch(item -> item.getProductId().equals(productId));

        if (!ownedByUser || !notCancelled || !containsProduct) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_PURCHASED);
        }
    }

    @Transactional(readOnly = true)
    public ReviewResponse.ReviewInfo getReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        return ReviewResponse.ReviewInfo.from(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse.ReviewInfo> getReviewsByProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
                .map(ReviewResponse.ReviewInfo::from);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse.ReviewInfo> getMyReviews(Long userId) {
        return reviewRepository.findByUserId(userId).stream()
                .map(ReviewResponse.ReviewInfo::from)
                .collect(Collectors.toList());
    }

    public ReviewResponse.ReviewInfo updateReview(Long reviewId, ReviewRequest.Update request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        review.update(request.getRating(), request.getContent());
        Review savedReview = reviewRepository.save(review);
        syncProductRating(review.getProductId());

        log.info("Review updated: reviewId={}", reviewId);

        return ReviewResponse.ReviewInfo.from(savedReview);
    }

    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        Long productId = review.getProductId();
        reviewRepository.delete(review);
        syncProductRating(productId);

        log.info("Review deleted: reviewId={}", reviewId);
    }

    private void syncProductRating(Long productId) {
        Double average = reviewRepository.findAverageRatingByProductId(productId);
        long count = reviewRepository.countByProductId(productId);
        BigDecimal averageRating = BigDecimal.valueOf(average != null ? average : 0.0)
                .setScale(2, RoundingMode.HALF_UP);

        productService.updateRatingStats(productId, averageRating, (int) count);
    }
}
