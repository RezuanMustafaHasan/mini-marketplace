package com.hasan.marketplace.repository;

import com.hasan.marketplace.entity.ProductReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    @EntityGraph(attributePaths = {"reviewer"})
    List<ProductReview> findByProductIdOrderByUpdatedAtDescIdDesc(Long productId);

    @EntityGraph(attributePaths = {"reviewer"})
    Optional<ProductReview> findByProductIdAndReviewerId(Long productId, Long reviewerId);

    @Query("select avg(review.rating) from ProductReview review where review.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    long countByProductId(Long productId);

    void deleteByProductId(Long productId);
}
