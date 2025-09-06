package com.ecommerce.msa.product.service;

import com.ecommerce.msa.product.dto.ProductRequest;
import com.ecommerce.msa.product.dto.ProductResponse;
import com.ecommerce.msa.product.entity.Product;
import com.ecommerce.msa.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public ProductResponse.ProductInfo createProduct(ProductRequest.Create request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(request.getCategory())
                .brand(request.getBrand())
                .imageUrl(request.getImageUrl())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully: {}", savedProduct.getName());

        return ProductResponse.ProductInfo.from(savedProduct);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#productId")
    public ProductResponse.ProductInfo getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다"));

        return ProductResponse.ProductInfo.from(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse.ProductInfo> getProducts(Pageable pageable) {
        Page<Product> products = productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
        return products.map(ProductResponse.ProductInfo::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse.ProductInfo> searchProducts(String keyword, Pageable pageable) {
        Page<Product> products = productRepository.searchByKeywordAndStatus(
                keyword, Product.ProductStatus.ACTIVE, pageable);
        return products.map(ProductResponse.ProductInfo::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse.ProductInfo> getProductsByCategory(String category, Pageable pageable) {
        Page<Product> products = productRepository.findByCategoryAndStatus(
                category, Product.ProductStatus.ACTIVE, pageable);
        return products.map(ProductResponse.ProductInfo::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse.ProductInfo> getProductsByBrand(String brand, Pageable pageable) {
        Page<Product> products = productRepository.findByBrandIgnoreCaseAndStatus(
                brand, Product.ProductStatus.ACTIVE, pageable);
        return products.map(ProductResponse.ProductInfo::from);
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return productRepository.findAllCategoriesByStatus(Product.ProductStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<String> getAllBrands() {
        return productRepository.findAllBrandsByStatus(Product.ProductStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse.ProductInfo> getLowStockProducts(Integer threshold) {
        List<Product> products = productRepository.findLowStockProducts(
                threshold != null ? threshold : 10, Product.ProductStatus.ACTIVE);
        return products.stream()
                .map(ProductResponse.ProductInfo::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse.ProductInfo> getProductsByPriceRange(
            BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Page<Product> products = productRepository.findByPriceRangeAndStatus(
                minPrice, maxPrice, Product.ProductStatus.ACTIVE, pageable);
        return products.map(ProductResponse.ProductInfo::from);
    }

    @CacheEvict(value = "products", key = "#productId")
    public ProductResponse.ProductInfo updateProduct(Long productId, ProductRequest.Update request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다"));

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }
        if (request.getBrand() != null) {
            product.setBrand(request.getBrand());
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully: {}", updatedProduct.getName());

        return ProductResponse.ProductInfo.from(updatedProduct);
    }

    @CacheEvict(value = "products", key = "#productId")
    public ProductResponse.StockInfo updateStock(Long productId, ProductRequest.StockUpdate request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다"));

        if ("INCREASE".equals(request.getOperation())) {
            product.increaseStock(request.getQuantity());
        } else if ("DECREASE".equals(request.getOperation())) {
            product.decreaseStock(request.getQuantity());
        } else {
            throw new RuntimeException("유효하지 않은 재고 작업입니다");
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product stock updated: {} {} {}", 
                updatedProduct.getName(), request.getOperation(), request.getQuantity());

        return ProductResponse.StockInfo.from(updatedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse.StockInfo checkStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다"));

        return ProductResponse.StockInfo.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse.StockInfo> checkMultipleStocks(List<Long> productIds) {
        List<Product> products = productRepository.findByProductIdIn(productIds);
        return products.stream()
                .map(ProductResponse.StockInfo::from)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "products", key = "#productId")
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다"));

        product.setStatus(Product.ProductStatus.INACTIVE);
        productRepository.save(product);
        log.info("Product deleted successfully: {}", product.getName());
    }
}
