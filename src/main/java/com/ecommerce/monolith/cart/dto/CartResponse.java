package com.ecommerce.monolith.cart.dto;

import com.ecommerce.monolith.cart.entity.Cart;
import com.ecommerce.monolith.cart.entity.CartItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class CartResponse {

    @Data
    @Builder
    public static class CartInfo {
        private Long cartId;
        private Long userId;
        private List<CartItemInfo> items;
        private BigDecimal totalAmount;

        public static CartInfo from(Cart cart) {
            List<CartItemInfo> items = cart.getItems().stream()
                    .map(CartItemInfo::from)
                    .collect(Collectors.toList());

            return CartInfo.builder()
                    .cartId(cart.getCartId())
                    .userId(cart.getUserId())
                    .items(items)
                    .totalAmount(cart.calculateTotalAmount())
                    .build();
        }
    }

    @Data
    @Builder
    public static class CartItemInfo {
        private Long cartItemId;
        private Long productId;
        private String productName;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;

        public static CartItemInfo from(CartItem item) {
            return CartItemInfo.builder()
                    .cartItemId(item.getCartItemId())
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .price(item.getPrice())
                    .quantity(item.getQuantity())
                    .subtotal(item.getSubtotal())
                    .build();
        }
    }
}
