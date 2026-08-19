package com.ecommerce.monolith.review.service;

import com.ecommerce.monolith.common.exception.BusinessException;
import com.ecommerce.monolith.common.exception.ErrorCode;
import com.ecommerce.monolith.order.dto.OrderResponse;
import com.ecommerce.monolith.order.entity.Order;
import com.ecommerce.monolith.order.service.OrderService;
import com.ecommerce.monolith.product.dto.ProductResponse;
import com.ecommerce.monolith.product.entity.Product;
import com.ecommerce.monolith.product.service.ProductService;
import com.ecommerce.monolith.review.dto.ReviewRequest;
import com.ecommerce.monolith.review.entity.Review;
import com.ecommerce.monolith.review.repository.ReviewRepository;
import com.ecommerce.monolith.user.dto.UserResponse;
import com.ecommerce.monolith.user.entity.User;
import com.ecommerce.monolith.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductService productService;

    @Mock
    private OrderService orderService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void createReviewSucceedsForVerifiedPurchaseAndSyncsProductRating() {
        ReviewRequest.Create request = new ReviewRequest.Create();
        request.setProductId(10L);
        request.setOrderId(100L);
        request.setRating(5);
        request.setContent("아주 좋아요");

        when(productService.getProduct(10L)).thenReturn(ProductResponse.ProductInfo.builder()
                .productId(10L)
                .status(Product.ProductStatus.ACTIVE)
                .build());
        when(orderService.getOrder(100L)).thenReturn(purchasedOrder(1L, 10L, Order.OrderStatus.DELIVERED));
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);
        when(userService.getUserById(1L)).thenReturn(UserResponse.UserInfo.builder()
                .userId(1L)
                .name("홍길동")
                .status(User.UserStatus.ACTIVE)
                .role(User.UserRole.USER)
                .build());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setReviewId(1000L);
            return review;
        });
        when(reviewRepository.findAverageRatingByProductId(10L)).thenReturn(5.0);
        when(reviewRepository.countByProductId(10L)).thenReturn(1L);

        var result = reviewService.createReview(1L, request);

        assertThat(result.getReviewId()).isEqualTo(1000L);
        assertThat(result.getReviewerName()).isEqualTo("홍길동");
        verify(productService).updateRatingStats(eq(10L), eq(BigDecimal.valueOf(5.00).setScale(2)), eq(1));
    }

    @Test
    void createReviewFailsWhenOrderBelongsToAnotherUser() {
        ReviewRequest.Create request = new ReviewRequest.Create();
        request.setProductId(10L);
        request.setOrderId(100L);
        request.setRating(5);
        request.setContent("좋아요");

        when(productService.getProduct(10L)).thenReturn(ProductResponse.ProductInfo.builder()
                .productId(10L)
                .build());
        when(orderService.getOrder(100L)).thenReturn(purchasedOrder(2L, 10L, Order.OrderStatus.DELIVERED));

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_PURCHASED);
    }

    @Test
    void createReviewFailsWhenOrderIsCancelled() {
        ReviewRequest.Create request = new ReviewRequest.Create();
        request.setProductId(10L);
        request.setOrderId(100L);
        request.setRating(5);
        request.setContent("좋아요");

        when(productService.getProduct(10L)).thenReturn(ProductResponse.ProductInfo.builder()
                .productId(10L)
                .build());
        when(orderService.getOrder(100L)).thenReturn(purchasedOrder(1L, 10L, Order.OrderStatus.CANCELLED));

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_PURCHASED);
    }

    @Test
    void createReviewFailsWhenOrderDoesNotContainProduct() {
        ReviewRequest.Create request = new ReviewRequest.Create();
        request.setProductId(999L);
        request.setOrderId(100L);
        request.setRating(5);
        request.setContent("좋아요");

        when(productService.getProduct(999L)).thenReturn(ProductResponse.ProductInfo.builder()
                .productId(999L)
                .build());
        when(orderService.getOrder(100L)).thenReturn(purchasedOrder(1L, 10L, Order.OrderStatus.DELIVERED));

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_PURCHASED);
    }

    @Test
    void createReviewFailsWhenAlreadyReviewed() {
        ReviewRequest.Create request = new ReviewRequest.Create();
        request.setProductId(10L);
        request.setOrderId(100L);
        request.setRating(5);
        request.setContent("좋아요");

        when(productService.getProduct(10L)).thenReturn(ProductResponse.ProductInfo.builder()
                .productId(10L)
                .build());
        when(orderService.getOrder(100L)).thenReturn(purchasedOrder(1L, 10L, Order.OrderStatus.DELIVERED));
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    @Test
    void deleteReviewRemovesReviewAndSyncsProductRating() {
        Review review = Review.builder()
                .reviewId(1000L)
                .userId(1L)
                .productId(10L)
                .orderId(100L)
                .rating(5)
                .build();

        when(reviewRepository.findById(1000L)).thenReturn(Optional.of(review));
        when(reviewRepository.findAverageRatingByProductId(10L)).thenReturn(0.0);
        when(reviewRepository.countByProductId(10L)).thenReturn(0L);

        reviewService.deleteReview(1000L);

        verify(reviewRepository).delete(review);
        verify(productService).updateRatingStats(10L, BigDecimal.valueOf(0.00).setScale(2), 0);
    }

    private OrderResponse.OrderInfo purchasedOrder(Long userId, Long productId, Order.OrderStatus status) {
        return OrderResponse.OrderInfo.builder()
                .orderId(100L)
                .userId(userId)
                .status(status)
                .orderItems(List.of(OrderResponse.OrderItemInfo.builder()
                        .productId(productId)
                        .quantity(1)
                        .price(BigDecimal.valueOf(1000))
                        .build()))
                .build();
    }
}
