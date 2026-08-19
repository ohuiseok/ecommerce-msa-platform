package com.ecommerce.monolith.order.controller;

import com.ecommerce.monolith.order.dto.OrderRequest;
import com.ecommerce.monolith.order.dto.OrderResponse;
import com.ecommerce.monolith.order.entity.Order;
import com.ecommerce.monolith.order.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "주문 생성, 조회, 상태 변경, 취소 API")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse.OrderInfo> createOrder(
            @Valid @RequestBody OrderRequest.Create request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        OrderResponse.OrderInfo orderInfo = orderService.createOrder(userId, request);
        return ResponseEntity.ok(orderInfo);
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse.OrderInfo> checkout(
            @Valid @RequestBody OrderRequest.Checkout request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        OrderResponse.OrderInfo orderInfo = orderService.createOrderFromCart(userId, request);
        return ResponseEntity.ok(orderInfo);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse.OrderInfo> getOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        OrderResponse.OrderInfo orderInfo = orderService.getOrder(orderId);
        validateSelfOrAdmin(orderInfo.getUserId(), authentication);
        return ResponseEntity.ok(orderInfo);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<OrderResponse.OrderInfo>> getOrdersByUserId(
            @PathVariable Long userId,
            Pageable pageable,
            Authentication authentication) {
        validateSelfOrAdmin(userId, authentication);
        Page<OrderResponse.OrderInfo> orders = orderService.getOrdersByUserId(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<OrderResponse.OrderInfo>> getOrdersByStatus(
            @PathVariable String status, Pageable pageable) {
        Order.OrderStatus orderStatus;
        try {
            orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        
        Page<OrderResponse.OrderInfo> orders = orderService.getOrdersByStatus(orderStatus, pageable);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse.OrderInfo> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderRequest.StatusUpdate request) {
        OrderResponse.OrderInfo orderInfo = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(orderInfo);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId, Authentication authentication) {
        OrderResponse.OrderInfo orderInfo = orderService.getOrder(orderId);
        validateSelfOrAdmin(orderInfo.getUserId(), authentication);
        orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order API is running");
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
