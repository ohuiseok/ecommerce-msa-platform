package com.ecommerce.monolith.review.dto;

import com.ecommerce.monolith.review.entity.Review;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

public class ReviewResponse {

    @Data
    @Builder
    public static class ReviewInfo {
        private Long reviewId;
        private Long userId;
        private String reviewerName;
        private Long productId;
        private Long orderId;
        private Integer rating;
        private String content;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static ReviewInfo from(Review review) {
            return ReviewInfo.builder()
                    .reviewId(review.getReviewId())
                    .userId(review.getUserId())
                    .reviewerName(review.getReviewerName())
                    .productId(review.getProductId())
                    .orderId(review.getOrderId())
                    .rating(review.getRating())
                    .content(review.getContent())
                    .createdAt(review.getCreatedAt())
                    .updatedAt(review.getUpdatedAt())
                    .build();
        }
    }
}
