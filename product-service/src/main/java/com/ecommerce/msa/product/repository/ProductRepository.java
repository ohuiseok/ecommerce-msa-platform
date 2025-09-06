package com.ecommerce.msa.product.repository;

import com.ecommerce.msa.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Page<Product> findByStatus(Product.ProductStatus status, Pageable pageable);
    
    Page<Product> findByCategory(String category, Pageable pageable);
    
    Page<Product> findByCategoryAndStatus(String category, Product.ProductStatus status, Pageable pageable);
    
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    Page<Product> findByNameContainingIgnoreCaseAndStatus(String name, Product.ProductStatus status, Pageable pageable);
    
    Page<Product> findByBrandIgnoreCase(String brand, Pageable pageable);
    
    Page<Product> findByBrandIgnoreCaseAndStatus(String brand, Product.ProductStatus status, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.status = :status")
    Page<Product> findByPriceRangeAndStatus(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("status") Product.ProductStatus status,
            Pageable pageable
    );
    
    @Query("SELECT p FROM Product p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "p.status = :status")
    Page<Product> searchByKeywordAndStatus(
            @Param("keyword") String keyword,
            @Param("status") Product.ProductStatus status,
            Pageable pageable
    );
    
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.status = :status ORDER BY p.category")
    List<String> findAllCategoriesByStatus(@Param("status") Product.ProductStatus status);
    
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.status = :status ORDER BY p.brand")
    List<String> findAllBrandsByStatus(@Param("status") Product.ProductStatus status);
    
    List<Product> findByProductIdIn(List<Long> productIds);
    
    @Query("SELECT p FROM Product p WHERE p.stockQuantity < :threshold AND p.status = :status")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold, @Param("status") Product.ProductStatus status);
}
