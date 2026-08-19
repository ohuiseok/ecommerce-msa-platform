package com.ecommerce.monolith.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class ReviewRequest {

    @Data
    public static class Create {
        @NotNull(message = "상품 ID는 필수입니다")
        private Long productId;

        @NotNull(message = "주문 ID는 필수입니다")
        private Long orderId;

        @NotNull(message = "평점은 필수입니다")
        @Min(value = 1, message = "평점은 1 이상이어야 합니다")
        @Max(value = 5, message = "평점은 5 이하여야 합니다")
        private Integer rating;

        @NotBlank(message = "리뷰 내용은 필수입니다")
        private String content;
    }

    @Data
    public static class Update {
        @NotNull(message = "평점은 필수입니다")
        @Min(value = 1, message = "평점은 1 이상이어야 합니다")
        @Max(value = 5, message = "평점은 5 이하여야 합니다")
        private Integer rating;

        @NotBlank(message = "리뷰 내용은 필수입니다")
        private String content;
    }
}
