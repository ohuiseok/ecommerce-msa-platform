package com.ecommerce.msa.order.client;

import com.ecommerce.msa.order.dto.ProductResponse;
import com.ecommerce.msa.order.dto.StockUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class ProductServiceClientFallback implements ProductServiceClient {
    
    @Override
    public ProductResponse getProductById(Long productId) {
        log.warn("Product service is unavailable. Using fallback for productId: {}", productId);
        return ProductResponse.builder()
                .productId(productId)
                .name("상품 정보 조회 실패")
                .price(BigDecimal.ZERO)
                .stockQuantity(0)
                .available(false)
                .build();
    }
    
    @Override
    public ProductResponse.StockInfo checkStock(Long productId) {
        log.warn("Product service is unavailable. Using fallback for stock check productId: {}", productId);
        return ProductResponse.StockInfo.builder()
                .productId(productId)
                .name("상품 정보 조회 실패")
                .stockQuantity(0)
                .available(false)
                .build();
    }
    
    @Override
    public ProductResponse.StockInfo updateStock(Long productId, StockUpdateRequest request) {
        log.warn("Product service is unavailable. Using fallback for stock update productId: {}", productId);
        return ProductResponse.StockInfo.builder()
                .productId(productId)
                .name("재고 업데이트 실패")
                .stockQuantity(0)
                .available(false)
                .build();
    }
}
