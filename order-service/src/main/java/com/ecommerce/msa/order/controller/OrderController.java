package com.ecommerce.msa.order.controller;

import com.ecommerce.msa.order.dto.OrderRequest;
import com.ecommerce.msa.order.dto.OrderResponse;
import com.ecommerce.msa.order.entity.Order;
import com.ecommerce.msa.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse.OrderInfo> createOrder(
            @Valid @RequestBody OrderRequest.Create request) {
        OrderResponse.OrderInfo orderInfo = orderService.createOrder(request);
        return ResponseEntity.ok(orderInfo);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse.OrderInfo> getOrder(@PathVariable Long orderId) {
        OrderResponse.OrderInfo orderInfo = orderService.getOrder(orderId);
        return ResponseEntity.ok(orderInfo);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<OrderResponse.OrderInfo>> getOrdersByUserId(
            @PathVariable Long userId, Pageable pageable) {
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
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service is running");
    }
}
