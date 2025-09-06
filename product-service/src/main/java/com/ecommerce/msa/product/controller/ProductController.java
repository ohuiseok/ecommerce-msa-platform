package com.ecommerce.msa.product.controller;

import com.ecommerce.msa.product.dto.ProductRequest;
import com.ecommerce.msa.product.dto.ProductResponse;
import com.ecommerce.msa.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse.ProductInfo> createProduct(
            @Valid @RequestBody ProductRequest.Create request) {
        ProductResponse.ProductInfo productInfo = productService.createProduct(request);
        return ResponseEntity.ok(productInfo);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse.ProductInfo> getProduct(@PathVariable Long productId) {
        ProductResponse.ProductInfo productInfo = productService.getProduct(productId);
        return ResponseEntity.ok(productInfo);
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse.ProductInfo>> getProducts(Pageable pageable) {
        Page<ProductResponse.ProductInfo> products = productService.getProducts(pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse.ProductInfo>> searchProducts(
            @RequestParam String keyword, Pageable pageable) {
        Page<ProductResponse.ProductInfo> products = productService.searchProducts(keyword, pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Page<ProductResponse.ProductInfo>> getProductsByCategory(
            @PathVariable String category, Pageable pageable) {
        Page<ProductResponse.ProductInfo> products = productService.getProductsByCategory(category, pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/brand/{brand}")
    public ResponseEntity<Page<ProductResponse.ProductInfo>> getProductsByBrand(
            @PathVariable String brand, Pageable pageable) {
        Page<ProductResponse.ProductInfo> products = productService.getProductsByBrand(brand, pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        List<String> categories = productService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/brands")
    public ResponseEntity<List<String>> getAllBrands() {
        List<String> brands = productService.getAllBrands();
        return ResponseEntity.ok(brands);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductResponse.ProductInfo>> getLowStockProducts(
            @RequestParam(required = false) Integer threshold) {
        List<ProductResponse.ProductInfo> products = productService.getLowStockProducts(threshold);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/price-range")
    public ResponseEntity<Page<ProductResponse.ProductInfo>> getProductsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            Pageable pageable) {
        Page<ProductResponse.ProductInfo> products = productService.getProductsByPriceRange(minPrice, maxPrice, pageable);
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse.ProductInfo> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequest.Update request) {
        ProductResponse.ProductInfo productInfo = productService.updateProduct(productId, request);
        return ResponseEntity.ok(productInfo);
    }

    @PutMapping("/{productId}/stock")
    public ResponseEntity<ProductResponse.StockInfo> updateStock(
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequest.StockUpdate request) {
        ProductResponse.StockInfo stockInfo = productService.updateStock(productId, request);
        return ResponseEntity.ok(stockInfo);
    }

    @GetMapping("/{productId}/stock")
    public ResponseEntity<ProductResponse.StockInfo> checkStock(@PathVariable Long productId) {
        ProductResponse.StockInfo stockInfo = productService.checkStock(productId);
        return ResponseEntity.ok(stockInfo);
    }

    @PostMapping("/stock/check")
    public ResponseEntity<List<ProductResponse.StockInfo>> checkMultipleStocks(
            @RequestBody List<Long> productIds) {
        List<ProductResponse.StockInfo> stockInfos = productService.checkMultipleStocks(productIds);
        return ResponseEntity.ok(stockInfos);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Product Service is running");
    }
}
