package com.ecommerce.msa.order.service;

import com.ecommerce.msa.order.client.ProductServiceClient;
import com.ecommerce.msa.order.client.UserServiceClient;
import com.ecommerce.msa.order.dto.*;
import com.ecommerce.msa.order.entity.Order;
import com.ecommerce.msa.order.entity.OrderItem;
import com.ecommerce.msa.order.entity.ShippingAddress;
import com.ecommerce.msa.order.event.OrderEvent;
import com.ecommerce.msa.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderResponse.OrderInfo createOrder(OrderRequest.Create request) {
        // 1. 사용자 정보 확인
        UserResponse userResponse = getUserWithFallback(request.getUserId());
        if (!userResponse.isAvailable()) {
            throw new RuntimeException("사용자 정보를 확인할 수 없습니다");
        }

        // 2. 주문 생성
        Order order = Order.builder()
                .userId(request.getUserId())
                .totalAmount(BigDecimal.ZERO)
                .shippingAddress(createShippingAddress(request.getShippingAddress()))
                .build();

        // 3. 주문 항목 처리
        for (OrderRequest.OrderItemRequest itemRequest : request.getOrderItems()) {
            ProductResponse productResponse = getProductWithFallback(itemRequest.getProductId());
            
            if (!productResponse.isAvailable()) {
                throw new RuntimeException("상품 정보를 확인할 수 없습니다: " + itemRequest.getProductId());
            }

            // 재고 확인 및 차감
            ProductResponse.StockInfo stockInfo = checkAndUpdateStock(
                    itemRequest.getProductId(), 
                    itemRequest.getQuantity()
            );
            
            if (!stockInfo.isAvailable()) {
                throw new RuntimeException("재고가 부족합니다: " + productResponse.getName());
            }

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .productName(productResponse.getName())
                    .price(productResponse.getPrice())
                    .quantity(itemRequest.getQuantity())
                    .build();

            order.addOrderItem(orderItem);
        }

        // 4. 총 금액 계산
        order.calculateTotalAmount();

        // 5. 주문 저장
        Order savedOrder = orderRepository.save(order);

        // 6. 주문 생성 이벤트 발행
        publishOrderEvent("ORDER_CREATED", savedOrder);

        log.info("Order created successfully: orderId={}, userId={}, totalAmount={}", 
                savedOrder.getOrderId(), savedOrder.getUserId(), savedOrder.getTotalAmount());

        return OrderResponse.OrderInfo.from(savedOrder);
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetUser")
    @Retry(name = "user-service")
    @TimeLimiter(name = "user-service")
    public CompletableFuture<UserResponse> getUserAsync(Long userId) {
        return CompletableFuture.supplyAsync(() -> userServiceClient.getUserById(userId));
    }

    public UserResponse getUserWithFallback(Long userId) {
        try {
            return getUserAsync(userId).get();
        } catch (Exception e) {
            log.error("Failed to get user info for userId: {}", userId, e);
            return UserResponse.builder()
                    .userId(userId)
                    .name("사용자 정보 조회 실패")
                    .available(false)
                    .build();
        }
    }

    @CircuitBreaker(name = "product-service", fallbackMethod = "fallbackGetProduct")
    @Retry(name = "product-service")
    @TimeLimiter(name = "product-service")
    public CompletableFuture<ProductResponse> getProductAsync(Long productId) {
        return CompletableFuture.supplyAsync(() -> productServiceClient.getProductById(productId));
    }

    public ProductResponse getProductWithFallback(Long productId) {
        try {
            return getProductAsync(productId).get();
        } catch (Exception e) {
            log.error("Failed to get product info for productId: {}", productId, e);
            return ProductResponse.builder()
                    .productId(productId)
                    .name("상품 정보 조회 실패")
                    .price(BigDecimal.ZERO)
                    .available(false)
                    .build();
        }
    }

    public ProductResponse.StockInfo checkAndUpdateStock(Long productId, Integer quantity) {
        try {
            StockUpdateRequest request = StockUpdateRequest.builder()
                    .quantity(quantity)
                    .operation("DECREASE")
                    .build();
            
            return productServiceClient.updateStock(productId, request);
        } catch (Exception e) {
            log.error("Failed to update stock for productId: {}, quantity: {}", productId, quantity, e);
            throw new RuntimeException("재고 업데이트 실패");
        }
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

        // 주문 상태 변경 이벤트 발행
        publishOrderEvent("ORDER_STATUS_UPDATED", updatedOrder);

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
            try {
                StockUpdateRequest request = StockUpdateRequest.builder()
                        .quantity(orderItem.getQuantity())
                        .operation("INCREASE")
                        .build();
                
                productServiceClient.updateStock(orderItem.getProductId(), request);
            } catch (Exception e) {
                log.error("Failed to restore stock for productId: {}, quantity: {}", 
                        orderItem.getProductId(), orderItem.getQuantity(), e);
            }
        }

        // 주문 취소 이벤트 발행
        publishOrderEvent("ORDER_CANCELLED", order);

        log.info("Order cancelled successfully: orderId={}", orderId);
    }

    private void publishOrderEvent(String eventType, Order order) {
        try {
            List<OrderEvent.OrderItemEvent> orderItemEvents = order.getOrderItems().stream()
                    .map(item -> OrderEvent.OrderItemEvent.builder()
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .build())
                    .collect(Collectors.toList());

            OrderEvent event = OrderEvent.builder()
                    .eventType(eventType)
                    .orderId(order.getOrderId())
                    .userId(order.getUserId())
                    .totalAmount(order.getTotalAmount())
                    .orderStatus(order.getStatus().name())
                    .orderItems(orderItemEvents)
                    .eventTime(LocalDateTime.now())
                    .build();

            kafkaTemplate.send("order.events", event);
            log.info("Order event published: eventType={}, orderId={}", eventType, order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish order event: eventType={}, orderId={}", eventType, order.getOrderId(), e);
        }
    }

    // Fallback methods
    public CompletableFuture<UserResponse> fallbackGetUser(Long userId, Exception ex) {
        log.warn("Fallback method called for getUserAsync: userId={}, error={}", userId, ex.getMessage());
        return CompletableFuture.completedFuture(
                UserResponse.builder()
                        .userId(userId)
                        .name("사용자 정보 조회 실패")
                        .available(false)
                        .build()
        );
    }

    public CompletableFuture<ProductResponse> fallbackGetProduct(Long productId, Exception ex) {
        log.warn("Fallback method called for getProductAsync: productId={}, error={}", productId, ex.getMessage());
        return CompletableFuture.completedFuture(
                ProductResponse.builder()
                        .productId(productId)
                        .name("상품 정보 조회 실패")
                        .price(BigDecimal.ZERO)
                        .available(false)
                        .build()
        );
    }
}
