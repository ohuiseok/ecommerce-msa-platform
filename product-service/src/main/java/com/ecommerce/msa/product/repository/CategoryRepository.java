package com.ecommerce.msa.product.repository;

import com.ecommerce.msa.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByName(String name);
    
    List<Category> findByParentCategoryId(Long parentCategoryId);
    
    List<Category> findByParentCategoryIdIsNull();
}
