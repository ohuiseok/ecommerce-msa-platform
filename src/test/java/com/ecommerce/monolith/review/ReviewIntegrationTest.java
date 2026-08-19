package com.ecommerce.monolith.review;

import com.ecommerce.monolith.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 구매(체크아웃) -> 리뷰 작성 -> 상품 평점 반영 -> 중복/미구매 리뷰 거부 -> 리뷰 삭제 시
 * 평점 재계산까지 실제 HTTP 계층과 PostgreSQL을 통해 검증하는 흐름 테스트.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.admin.email=review-admin@test.com",
        "app.admin.password=admin1234",
        "app.admin.name=Test Admin"
})
class ReviewIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createReviewUpdatesProductRatingAndRejectsDuplicateOrUnpurchasedReview() throws Exception {
        String userToken = registerAndLogin("review-buyer@example.com");
        String adminToken = login("review-admin@test.com", "admin1234");

        Long purchasedProductId = createProduct(adminToken, "노이즈캔슬링 헤드폰", 100000, 10);
        Long otherProductId = createProduct(adminToken, "마우스패드", 5000, 10);

        addToCart(userToken, purchasedProductId, 1);
        Long orderId = checkout(userToken);

        String reviewResponse = mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d, "orderId": %d, "rating": 5, "content": "소리가 정말 좋아요"}
                                """.formatted(purchasedProductId, orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewerName").value("Buyer"))
                .andReturn().getResponse().getContentAsString();

        Long reviewId = objectMapper.readTree(reviewResponse).get("reviewId").asLong();

        mockMvc.perform(get("/api/products/{productId}", purchasedProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(5.0))
                .andExpect(jsonPath("$.reviewCount").value(1));

        mockMvc.perform(get("/api/reviews/product/{productId}", purchasedProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reviewId").value(reviewId));

        // 같은 상품에 대한 중복 리뷰는 거부되어야 한다
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d, "orderId": %d, "rating": 3, "content": "다시 씁니다"}
                                """.formatted(purchasedProductId, orderId)))
                .andExpect(status().isConflict());

        // 이 주문으로 구매하지 않은 상품에 대한 리뷰는 거부되어야 한다
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d, "orderId": %d, "rating": 4, "content": "구매 안 한 상품"}
                                """.formatted(otherProductId, orderId)))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/{productId}", purchasedProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.reviewCount").value(0));
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "password123", "name": "Buyer", "phoneNumber": "010-1234-5678"}
                                """.formatted(email)))
                .andExpect(status().isOk());

        return login(email, "password123");
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private Long createProduct(String adminToken, String name, int price, int stock) throws Exception {
        String response = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "price": %d, "stockQuantity": %d, "category": "electronics", "brand": "TestBrand"}
                                """.formatted(name, price, stock)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("productId").asLong();
    }

    private void addToCart(String userToken, Long productId, int quantity) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d, "quantity": %d}
                                """.formatted(productId, quantity)))
                .andExpect(status().isOk());
    }

    private Long checkout(String userToken) throws Exception {
        String response = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress": {"zipCode": "12345", "address": "Seoul", "recipientName": "Buyer", "recipientPhone": "010-1234-5678"}}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("orderId").asLong();
    }
}
