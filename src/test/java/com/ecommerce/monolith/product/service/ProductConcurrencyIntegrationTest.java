package com.ecommerce.monolith.product.service;

import com.ecommerce.monolith.product.dto.ProductRequest;
import com.ecommerce.monolith.product.entity.Product;
import com.ecommerce.monolith.product.repository.ProductRepository;
import com.ecommerce.monolith.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * README에 남아있던 '동시 주문 시나리오 테스트'를 검증한다.
 * 재고보다 많은 동시 요청이 들어와도 조건부 UPDATE 쿼리 덕분에 재고가 음수로 내려가지 않아야 한다.
 */
class ProductConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void concurrentDecreaseStockNeverOversells() throws InterruptedException {
        int initialStock = 10;
        int concurrentRequests = 30;

        Product product = productRepository.save(Product.builder()
                .name("한정판 스니커즈")
                .price(BigDecimal.valueOf(150000))
                .stockQuantity(initialStock)
                .status(Product.ProductStatus.ACTIVE)
                .build());

        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        CompletableFuture<?>[] tasks = new CompletableFuture[concurrentRequests];
        for (int i = 0; i < concurrentRequests; i++) {
            tasks[i] = CompletableFuture.runAsync(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                ProductRequest.StockUpdate request = new ProductRequest.StockUpdate();
                request.setQuantity(1);
                request.setOperation(ProductRequest.Operation.DECREASE);

                try {
                    productService.updateStock(product.getProductId(), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }, executor);
        }

        readyLatch.await();
        startLatch.countDown();
        CompletableFuture.allOf(tasks).join();
        executor.shutdown();

        Product finalProduct = productRepository.findById(product.getProductId()).orElseThrow();

        assertThat(successCount.get()).isEqualTo(initialStock);
        assertThat(failureCount.get()).isEqualTo(concurrentRequests - initialStock);
        assertThat(finalProduct.getStockQuantity()).isZero();
        assertThat(finalProduct.getStatus()).isEqualTo(Product.ProductStatus.OUT_OF_STOCK);
    }
}
