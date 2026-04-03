package com.hasan.marketplace.repository;

import com.hasan.marketplace.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Override
    @EntityGraph(attributePaths = {"seller", "category"})
    List<Product> findAll();

    @Override
    @EntityGraph(attributePaths = {"seller", "category"})
    java.util.Optional<Product> findById(Long id);

    @EntityGraph(attributePaths = {"seller", "category"})
    List<Product> findBySellerIdOrderByIdDesc(Long sellerId);

    @EntityGraph(attributePaths = {"seller", "category"})
    List<Product> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"seller", "category"})
    List<Product> findByNameContainingIgnoreCaseOrderByIdDesc(String keyword);

    @EntityGraph(attributePaths = {"seller", "category"})
    List<Product> findByCategoryIdOrderByIdDesc(Long categoryId);

    @EntityGraph(attributePaths = {"seller", "category"})
    List<Product> findByNameContainingIgnoreCaseAndCategoryIdOrderByIdDesc(String keyword, Long categoryId);

    List<Product> findByCategoryIsNull();

    long countBySellerId(Long sellerId);

    long countByCategoryId(Long categoryId);

    boolean existsByCategoryId(Long categoryId);
}
