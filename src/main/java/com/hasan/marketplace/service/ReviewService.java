package com.hasan.marketplace.service;

import com.hasan.marketplace.dto.ProductReviewRequest;
import com.hasan.marketplace.dto.ProductReviewResponse;
import java.util.List;
import java.util.Optional;

public interface ReviewService {

    List<ProductReviewResponse> getReviewsForProduct(Long productId);

    Optional<ProductReviewResponse> getReviewForProductByReviewer(Long productId, Long reviewerId);

    boolean canUserReviewProduct(Long productId, Long userId);

    ProductReviewResponse createOrUpdateReview(Long productId, ProductReviewRequest request, Long reviewerId);

    void deleteReviewsForProduct(Long productId);
}
