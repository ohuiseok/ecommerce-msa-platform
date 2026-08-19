package com.ecommerce.monolith.cart.service;

import com.ecommerce.monolith.cart.dto.CartRequest;
import com.ecommerce.monolith.cart.dto.CartResponse;
import com.ecommerce.monolith.cart.entity.Cart;
import com.ecommerce.monolith.cart.repository.CartRepository;
import com.ecommerce.monolith.product.dto.ProductResponse;
import com.ecommerce.monolith.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final ProductService productService;

    public CartResponse.CartInfo getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return CartResponse.CartInfo.from(cart);
    }

    public CartResponse.CartInfo addItem(Long userId, CartRequest.AddItem request) {
        Cart cart = getOrCreateCart(userId);
        ProductResponse.ProductInfo product = productService.getProduct(request.getProductId());

        cart.addItem(product.getProductId(), product.getName(), product.getPrice(), request.getQuantity());
        Cart savedCart = cartRepository.save(cart);

        log.info("Item added to cart: userId={}, productId={}, quantity={}",
                userId, request.getProductId(), request.getQuantity());

        return CartResponse.CartInfo.from(savedCart);
    }

    public CartResponse.CartInfo updateItemQuantity(Long userId, Long cartItemId, CartRequest.UpdateItem request) {
        Cart cart = getOrCreateCart(userId);
        cart.updateItemQuantity(cartItemId, request.getQuantity());
        Cart savedCart = cartRepository.save(cart);

        return CartResponse.CartInfo.from(savedCart);
    }

    public CartResponse.CartInfo removeItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);
        cart.removeItem(cartItemId);
        Cart savedCart = cartRepository.save(cart);

        return CartResponse.CartInfo.from(savedCart);
    }

    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.clear();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));
    }
}
