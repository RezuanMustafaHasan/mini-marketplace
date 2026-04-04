package com.hasan.marketplace.service.impl;

import com.hasan.marketplace.dto.ProductReviewRequest;
import com.hasan.marketplace.dto.ProductReviewResponse;
import com.hasan.marketplace.entity.Product;
import com.hasan.marketplace.entity.ProductReview;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.exception.ResourceNotFoundException;
import com.hasan.marketplace.exception.UnauthorizedActionException;
import com.hasan.marketplace.repository.CustomerOrderRepository;
import com.hasan.marketplace.repository.ProductRepository;
import com.hasan.marketplace.repository.ProductReviewRepository;
import com.hasan.marketplace.repository.UserRepository;
import com.hasan.marketplace.service.ReviewService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CustomerOrderRepository customerOrderRepository;

    @Override
    public List<ProductReviewResponse> getReviewsForProduct(Long productId) {
        validateProductExists(productId);

        return productReviewRepository.findByProductIdOrderByUpdatedAtDescIdDesc(productId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public Optional<ProductReviewResponse> getReviewForProductByReviewer(Long productId, Long reviewerId) {
        validateProductExists(productId);

        return productReviewRepository.findByProductIdAndReviewerId(productId, reviewerId)
                .map(this::mapToResponse);
    }

    @Override
    public boolean canUserReviewProduct(Long productId, Long userId) {
        if (productId == null || userId == null) {
            return false;
        }

        return customerOrderRepository.existsByBuyerIdAndProductId(userId, productId);
    }

    @Override
    @Transactional
    public ProductReviewResponse createOrUpdateReview(Long productId, ProductReviewRequest request, Long reviewerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + reviewerId));

        if (!customerOrderRepository.existsByBuyerIdAndProductId(reviewerId, productId)) {
            throw new UnauthorizedActionException("You can review only products you have purchased.");
        }

        LocalDateTime now = LocalDateTime.now();
        ProductReview review = productReviewRepository.findByProductIdAndReviewerId(productId, reviewerId)
                .orElseGet(() -> ProductReview.builder()
                        .product(product)
                        .reviewer(reviewer)
                        .createdAt(now)
                        .build());

        review.setRating(request.getRating());
        review.setComment(normalizeComment(request.getComment()));
        review.setUpdatedAt(now);

        if (review.getCreatedAt() == null) {
            review.setCreatedAt(now);
        }

        ProductReview savedReview = productReviewRepository.save(review);
        return mapToResponse(savedReview);
    }

    @Override
    @Transactional
    public void deleteReviewsForProduct(Long productId) {
        productReviewRepository.deleteByProductId(productId);
    }

    private void validateProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
    }

    private ProductReviewResponse mapToResponse(ProductReview review) {
        return ProductReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
