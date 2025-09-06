package com.ecommerce.msa.order.client;

import com.ecommerce.msa.order.dto.ProductResponse;
import com.ecommerce.msa.order.dto.StockUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service", fallback = ProductServiceClientFallback.class)
public interface ProductServiceClient {
    
    @GetMapping("/products/{productId}")
    ProductResponse getProductById(@PathVariable Long productId);
    
    @GetMapping("/products/{productId}/stock")
    ProductResponse.StockInfo checkStock(@PathVariable Long productId);
    
    @PutMapping("/products/{productId}/stock")
    ProductResponse.StockInfo updateStock(@PathVariable Long productId, @RequestBody StockUpdateRequest request);
}
