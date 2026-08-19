package com.ecommerce.monolith.order.service;

import com.ecommerce.monolith.order.dto.OrderRequest;
import com.ecommerce.monolith.order.dto.OrderResponse;
import com.ecommerce.monolith.order.entity.Order;
import com.ecommerce.monolith.order.entity.OrderItem;
import com.ecommerce.monolith.order.entity.ShippingAddress;
import com.ecommerce.monolith.order.repository.OrderRepository;
import com.ecommerce.monolith.product.dto.ProductRequest;
import com.ecommerce.monolith.product.dto.ProductResponse;
import com.ecommerce.monolith.product.service.ProductService;
import com.ecommerce.monolith.user.dto.UserResponse;
import com.ecommerce.monolith.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserService userService;
    private final ProductService productService;

    public OrderResponse.OrderInfo createOrder(Long userId, OrderRequest.Create request) {
        // 1. 사용자 정보 확인
        UserResponse.UserInfo userInfo = userService.getUserById(userId);

        // 2. 주문 생성
        Order order = Order.builder()
                .userId(userInfo.getUserId())
                .totalAmount(BigDecimal.ZERO)
                .shippingAddress(createShippingAddress(request.getShippingAddress()))
                .build();

        // 3. 주문 항목 처리
        for (OrderRequest.OrderItemRequest itemRequest : request.getOrderItems()) {
            ProductResponse.ProductInfo productInfo = productService.getProduct(itemRequest.getProductId());

            // 재고 확인 및 차감
            decreaseStock(
                    itemRequest.getProductId(), 
                    itemRequest.getQuantity()
            );

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .productName(productInfo.getName())
                    .price(productInfo.getPrice())
                    .quantity(itemRequest.getQuantity())
                    .build();

            order.addOrderItem(orderItem);
        }

        // 4. 총 금액 계산
        order.calculateTotalAmount();

        // 5. 주문 저장
        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully: orderId={}, userId={}, totalAmount={}", 
                savedOrder.getOrderId(), savedOrder.getUserId(), savedOrder.getTotalAmount());

        return OrderResponse.OrderInfo.from(savedOrder);
    }

    private ProductResponse.StockInfo decreaseStock(Long productId, Integer quantity) {
        ProductRequest.StockUpdate request = new ProductRequest.StockUpdate();
        request.setQuantity(quantity);
        request.setOperation("DECREASE");

        return productService.updateStock(productId, request);
    }

    private ShippingAddress createShippingAddress(OrderRequest.ShippingAddressRequest request) {
        return ShippingAddress.builder()
                .zipCode(request.getZipCode())
                .address(request.getAddress())
                .detailAddress(request.getDetailAddress())
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .build();
    }

    @Transactional(readOnly = true)
    public OrderResponse.OrderInfo getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다"));

        return OrderResponse.OrderInfo.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse.OrderInfo> getOrdersByUserId(Long userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return orders.map(OrderResponse.OrderInfo::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse.OrderInfo> getOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
        Page<Order> orders = orderRepository.findByStatus(status, pageable);
        return orders.map(OrderResponse.OrderInfo::from);
    }

    public OrderResponse.OrderInfo updateOrderStatus(Long orderId, OrderRequest.StatusUpdate request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다"));

        Order.OrderStatus newStatus;
        try {
            newStatus = Order.OrderStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("유효하지 않은 주문 상태입니다: " + request.getStatus());
        }

        order.updateStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order status updated: orderId={}, status={}", orderId, newStatus);

        return OrderResponse.OrderInfo.from(updatedOrder);
    }

    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다"));

        if (order.getStatus() == Order.OrderStatus.SHIPPED || 
            order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new RuntimeException("배송 중이거나 완료된 주문은 취소할 수 없습니다");
        }

        order.updateStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        // 재고 복원
        for (OrderItem orderItem : order.getOrderItems()) {
            ProductRequest.StockUpdate request = new ProductRequest.StockUpdate();
            request.setQuantity(orderItem.getQuantity());
            request.setOperation("INCREASE");

            productService.updateStock(orderItem.getProductId(), request);
        }

        log.info("Order cancelled successfully: orderId={}", orderId);
    }
}
