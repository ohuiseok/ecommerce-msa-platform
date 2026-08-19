package com.ecommerce.monolith.order.service;

import com.ecommerce.monolith.order.dto.OrderRequest;
import com.ecommerce.monolith.order.dto.OrderResponse;
import com.ecommerce.monolith.order.entity.Order;
import com.ecommerce.monolith.order.repository.OrderRepository;
import com.ecommerce.monolith.product.dto.ProductResponse;
import com.ecommerce.monolith.product.entity.Product;
import com.ecommerce.monolith.product.service.ProductService;
import com.ecommerce.monolith.user.dto.UserResponse;
import com.ecommerce.monolith.user.entity.User;
import com.ecommerce.monolith.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderUsesAuthenticatedUserId() {
        OrderRequest.Create request = new OrderRequest.Create();
        request.setUserId(999L);
        request.setOrderItems(List.of(orderItemRequest()));
        request.setShippingAddress(shippingAddressRequest());

        when(userService.getUserById(1L)).thenReturn(UserResponse.UserInfo.builder()
                .userId(1L)
                .email("user@example.com")
                .name("User")
                .status(User.UserStatus.ACTIVE)
                .role(User.UserRole.USER)
                .build());
        when(productService.getProduct(10L)).thenReturn(ProductResponse.ProductInfo.builder()
                .productId(10L)
                .name("Phone")
                .price(BigDecimal.valueOf(1000))
                .stockQuantity(5)
                .status(Product.ProductStatus.ACTIVE)
                .build());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse.OrderInfo result = orderService.createOrder(1L, request);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(orderCaptor.getValue().getUserId()).isEqualTo(1L);
    }

    private OrderRequest.OrderItemRequest orderItemRequest() {
        OrderRequest.OrderItemRequest request = new OrderRequest.OrderItemRequest();
        request.setProductId(10L);
        request.setQuantity(2);
        return request;
    }

    private OrderRequest.ShippingAddressRequest shippingAddressRequest() {
        OrderRequest.ShippingAddressRequest request = new OrderRequest.ShippingAddressRequest();
        request.setZipCode("12345");
        request.setAddress("Seoul");
        request.setRecipientName("User");
        request.setRecipientPhone("010-1234-5678");
        return request;
    }
}
