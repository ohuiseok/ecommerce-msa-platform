package com.ecommerce.monolith.cart.service;

import com.ecommerce.monolith.cart.dto.CartRequest;
import com.ecommerce.monolith.cart.dto.CartResponse;
import com.ecommerce.monolith.cart.entity.Cart;
import com.ecommerce.monolith.cart.repository.CartRepository;
import com.ecommerce.monolith.common.exception.BusinessException;
import com.ecommerce.monolith.common.exception.ErrorCode;
import com.ecommerce.monolith.product.dto.ProductResponse;
import com.ecommerce.monolith.product.entity.Product;
import com.ecommerce.monolith.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private CartService cartService;

    @Test
    void addItemMergesQuantityWhenProductAlreadyInCart() {
        Cart cart = Cart.builder().cartId(1L).userId(1L).build();
        cart.addItem(10L, "Phone", BigDecimal.valueOf(1000), 1);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productService.getProduct(10L)).thenReturn(ProductResponse.ProductInfo.builder()
                .productId(10L)
                .name("Phone")
                .price(BigDecimal.valueOf(1000))
                .status(Product.ProductStatus.ACTIVE)
                .build());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartRequest.AddItem request = new CartRequest.AddItem();
        request.setProductId(10L);
        request.setQuantity(2);

        CartResponse.CartInfo result = cartService.addItem(1L, request);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
    }

    @Test
    void addItemCreatesCartWhenNoneExists() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            cart.setCartId(1L);
            return cart;
        });
        when(productService.getProduct(10L)).thenReturn(ProductResponse.ProductInfo.builder()
                .productId(10L)
                .name("Phone")
                .price(BigDecimal.valueOf(1000))
                .status(Product.ProductStatus.ACTIVE)
                .build());

        CartRequest.AddItem request = new CartRequest.AddItem();
        request.setProductId(10L);
        request.setQuantity(1);

        CartResponse.CartInfo result = cartService.addItem(1L, request);

        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    void removeItemThrowsWhenItemNotFound() {
        Cart cart = Cart.builder().cartId(1L).userId(1L).build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.removeItem(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_NOT_FOUND);
    }
}
