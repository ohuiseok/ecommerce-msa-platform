package com.ecommerce.monolith.cart.controller;

import com.ecommerce.monolith.cart.dto.CartRequest;
import com.ecommerce.monolith.cart.dto.CartResponse;
import com.ecommerce.monolith.cart.service.CartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "장바구니 조회 및 상품 담기/수정/삭제 API")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse.CartInfo> getCart(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse.CartInfo> addItem(
            @Valid @RequestBody CartRequest.AddItem request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(cartService.addItem(userId, request));
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse.CartInfo> updateItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartRequest.UpdateItem request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, cartItemId, request));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse.CartInfo> removeItem(
            @PathVariable Long cartItemId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(cartService.removeItem(userId, cartItemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Cart API is running");
    }
}
