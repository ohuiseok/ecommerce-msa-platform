package com.ecommerce.monolith.order.service;

import com.ecommerce.monolith.cart.dto.CartResponse;
import com.ecommerce.monolith.cart.service.CartService;
import com.ecommerce.monolith.common.exception.BusinessException;
import com.ecommerce.monolith.common.exception.ErrorCode;
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
    private final CartService cartService;

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
            addOrderItem(order, itemRequest.getProductId(), itemRequest.getQuantity());
        }

        // 4. 총 금액 계산
        order.calculateTotalAmount();

        // 5. 주문 저장
        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully: orderId={}, userId={}, totalAmount={}",
                savedOrder.getOrderId(), savedOrder.getUserId(), savedOrder.getTotalAmount());

        return OrderResponse.OrderInfo.from(savedOrder);
    }

    public OrderResponse.OrderInfo createOrderFromCart(Long userId, OrderRequest.Checkout request) {
        UserResponse.UserInfo userInfo = userService.getUserById(userId);
        CartResponse.CartInfo cart = cartService.getCart(userId);

        if (cart.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        Order order = Order.builder()
                .userId(userInfo.getUserId())
                .totalAmount(BigDecimal.ZERO)
                .shippingAddress(createShippingAddress(request.getShippingAddress()))
                .build();

        for (CartResponse.CartItemInfo item : cart.getItems()) {
            addOrderItem(order, item.getProductId(), item.getQuantity());
        }

        order.calculateTotalAmount();
        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(userId);

        log.info("Order created from cart: orderId={}, userId={}, totalAmount={}",
                savedOrder.getOrderId(), savedOrder.getUserId(), savedOrder.getTotalAmount());

        return OrderResponse.OrderInfo.from(savedOrder);
    }

    private void addOrderItem(Order order, Long productId, Integer quantity) {
        ProductResponse.ProductInfo productInfo = productService.getProduct(productId);

        // 재고 확인 및 차감
        decreaseStock(productId, quantity);

        OrderItem orderItem = OrderItem.builder()
                .productId(productId)
                .productName(productInfo.getName())
                .price(productInfo.getPrice())
                .quantity(quantity)
                .build();

        order.addOrderItem(orderItem);
    }

    private ProductResponse.StockInfo decreaseStock(Long productId, Integer quantity) {
        ProductRequest.StockUpdate request = new ProductRequest.StockUpdate();
        request.setQuantity(quantity);
        request.setOperation(ProductRequest.Operation.DECREASE);

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
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

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
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        Order.OrderStatus newStatus;
        try {
            newStatus = Order.OrderStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS, "유효하지 않은 주문 상태입니다: " + request.getStatus());
        }

        order.updateStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order status updated: orderId={}, status={}", orderId, newStatus);

        return OrderResponse.OrderInfo.from(updatedOrder);
    }

    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
            order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.ORDER_CANCELLATION_NOT_ALLOWED);
        }

        order.updateStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        // 재고 복원
        for (OrderItem orderItem : order.getOrderItems()) {
            ProductRequest.StockUpdate request = new ProductRequest.StockUpdate();
            request.setQuantity(orderItem.getQuantity());
            request.setOperation(ProductRequest.Operation.INCREASE);

            productService.updateStock(orderItem.getProductId(), request);
        }

        log.info("Order cancelled successfully: orderId={}", orderId);
    }

    @Transactional
    public void markOrderConfirmed(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.updateStatus(Order.OrderStatus.CONFIRMED);
        orderRepository.save(order);

        log.info("Order confirmed after payment: orderId={}", orderId);
    }
}
