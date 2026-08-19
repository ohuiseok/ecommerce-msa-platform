package com.ecommerce.monolith.cart.entity;

import com.ecommerce.monolith.common.exception.BusinessException;
import com.ecommerce.monolith.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    @Column(nullable = false, unique = true)
    private Long userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addItem(Long productId, String productName, BigDecimal price, Integer quantity) {
        items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresentOrElse(
                        existing -> {
                            existing.setPrice(price);
                            existing.setProductName(productName);
                            existing.setQuantity(existing.getQuantity() + quantity);
                        },
                        () -> {
                            CartItem item = CartItem.builder()
                                    .productId(productId)
                                    .productName(productName)
                                    .price(price)
                                    .quantity(quantity)
                                    .build();
                            items.add(item);
                            item.setCart(this);
                        }
                );
    }

    public void updateItemQuantity(Long cartItemId, Integer quantity) {
        CartItem item = findItem(cartItemId);
        item.setQuantity(quantity);
    }

    public void removeItem(Long cartItemId) {
        CartItem item = findItem(cartItemId);
        items.remove(item);
        item.setCart(null);
    }

    public void clear() {
        items.forEach(item -> item.setCart(null));
        items.clear();
    }

    public BigDecimal calculateTotalAmount() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CartItem findItem(Long cartItemId) {
        return items.stream()
                .filter(item -> item.getCartItemId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
    }
}
