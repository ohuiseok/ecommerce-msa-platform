package com.ecommerce.monolith.coupon;

import com.ecommerce.monolith.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 쿠폰 발급 -> 체크아웃 적용 -> 주문 취소 시 쿠폰 복원까지
 * 실제 HTTP 계층과 PostgreSQL을 통해 검증하는 전체 흐름 테스트.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.admin.email=coupon-admin@test.com",
        "app.admin.password=admin1234",
        "app.admin.name=Test Admin"
})
class CouponCheckoutIntegrationTest extends AbstractIntegrationTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void checkoutWithCouponAppliesDiscountAndCancelRestoresCoupon() throws Exception {
        String userToken = registerAndLogin("coupon-buyer@example.com");
        String adminToken = login("coupon-admin@test.com", "admin1234");

        Long productId = createProduct(adminToken, "블루투스 스피커", 20000, 10);
        Long couponId = createCoupon(adminToken, "WELCOME5000", 5000, 10000, 10);

        Long userCouponId = issueCoupon(userToken, couponId);

        addToCart(userToken, productId, 2); // 20000 * 2 = 40000

        String checkoutResponse = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress": {"zipCode": "12345", "address": "Seoul", "recipientName": "Buyer", "recipientPhone": "010-1234-5678"}, "userCouponId": %d}
                                """.formatted(userCouponId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalAmount").value(40000))
                .andExpect(jsonPath("$.discountAmount").value(5000))
                .andExpect(jsonPath("$.totalAmount").value(35000))
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(checkoutResponse).get("orderId").asLong();

        // 쿠폰은 이미 사용되어 재사용이 거부되어야 한다
        mockMvc.perform(post("/api/coupons/" + couponId + "/issue")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/coupons/my")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ISSUED"))
                .andExpect(jsonPath("$[0].orderId").doesNotExist());
    }

    @Test
    void checkoutFailsWhenCouponMinOrderAmountNotMet() throws Exception {
        String userToken = registerAndLogin("coupon-buyer2@example.com");
        String adminToken = login("coupon-admin@test.com", "admin1234");

        Long productId = createProduct(adminToken, "USB 케이블", 3000, 10);
        Long couponId = createCoupon(adminToken, "MIN30000", 5000, 30000, 10);
        Long userCouponId = issueCoupon(userToken, couponId);

        addToCart(userToken, productId, 1);

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress": {"zipCode": "12345", "address": "Seoul", "recipientName": "Buyer", "recipientPhone": "010-1234-5678"}, "userCouponId": %d}
                                """.formatted(userCouponId)))
                .andExpect(status().isConflict());
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

    private Long createCoupon(String adminToken, String code, int discountValue, int minOrderAmount, int issueLimit) throws Exception {
        String validFrom = LocalDateTime.now().minusDays(1).format(ISO);
        String validUntil = LocalDateTime.now().plusDays(30).format(ISO);

        String response = mockMvc.perform(post("/api/coupons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s", "name": "%s", "discountType": "FIXED_AMOUNT", "discountValue": %d,
                                 "minOrderAmount": %d, "validFrom": "%s", "validUntil": "%s", "issueLimit": %d}
                                """.formatted(code, code, discountValue, minOrderAmount, validFrom, validUntil, issueLimit)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("couponId").asLong();
    }

    private Long issueCoupon(String userToken, Long couponId) throws Exception {
        String response = mockMvc.perform(post("/api/coupons/{couponId}/issue", couponId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("userCouponId").asLong();
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
}
