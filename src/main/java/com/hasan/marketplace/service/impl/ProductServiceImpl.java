package com.hasan.marketplace.service.impl;

import com.hasan.marketplace.dto.ProductRequest;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.entity.Category;
import com.hasan.marketplace.entity.Product;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.exception.InvalidCategoryException;
import com.hasan.marketplace.exception.ResourceNotFoundException;
import com.hasan.marketplace.exception.UnauthorizedActionException;
import com.hasan.marketplace.repository.CategoryRepository;
import com.hasan.marketplace.repository.ProductRepository;
import com.hasan.marketplace.repository.ProductReviewRepository;
import com.hasan.marketplace.repository.UserRepository;
import com.hasan.marketplace.service.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductReviewRepository productReviewRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, Long sellerId) {
        User seller = getSellerById(sellerId);
        Category category = getCategoryById(request.getCategoryId());

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setSeller(seller);

        Product savedProduct = productRepository.save(product);
        return mapToProductResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request, Long sellerId) {
        Product product = getProductByIdOrThrow(productId);
        validateOwnership(product, sellerId);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(getCategoryById(request.getCategoryId()));

        Product updatedProduct = productRepository.save(product);
        return mapToProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId, Long sellerId) {
        Product product = getProductByIdOrThrow(productId);
        validateOwnership(product, sellerId);
        productReviewRepository.deleteByProductId(productId);
        productRepository.delete(product);
    }

    @Override
    public ProductResponse getProductById(Long productId) {
        return mapToProductResponse(getProductByIdOrThrow(productId));
    }

    @Override
    public ProductResponse getProductByIdForSeller(Long productId, Long sellerId) {
        Product product = getProductByIdOrThrow(productId);
        validateOwnership(product, sellerId);
        return mapToProductResponse(product);
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToProductResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> getProductsBySeller(Long sellerId) {
        return productRepository.findBySellerIdOrderByIdDesc(sellerId)
                .stream()
                .map(this::mapToProductResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> searchProducts(String keyword, Long categoryId) {
        String normalizedKeyword = normalizeKeyword(keyword);
        validateCategoryFilter(categoryId);

        List<Product> products;

        if (normalizedKeyword == null && categoryId == null) {
            products = productRepository.findAllByOrderByIdDesc();
        } else if (normalizedKeyword == null) {
            products = productRepository.findByCategoryIdOrderByIdDesc(categoryId);
        } else if (categoryId == null) {
            products = productRepository
                .findByNameContainingIgnoreCaseOrSeller_FullNameContainingIgnoreCaseOrderByIdDesc(
                    normalizedKeyword,
                    normalizedKeyword
                );
        } else {
            products = productRepository
                .findByCategoryIdAndNameContainingIgnoreCaseOrCategoryIdAndSeller_FullNameContainingIgnoreCaseOrderByIdDesc(
                    categoryId,
                    normalizedKeyword,
                    categoryId,
                    normalizedKeyword
                );
        }

        return products
                .stream()
                .map(this::mapToProductResponse)
                .toList();
    }

    @Override
    public long countProductsBySeller(Long sellerId) {
        return productRepository.countBySellerId(sellerId);
    }

    private User getSellerById(Long sellerId) {
        return userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));
    }

    private Category getCategoryById(Long categoryId) {
        if (categoryId == null) {
            throw new InvalidCategoryException("Please select a valid category.");
        }

        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new InvalidCategoryException("Selected category does not exist."));
    }

    private void validateCategoryFilter(Long categoryId) {
        if (categoryId != null && categoryRepository.findById(categoryId).isEmpty()) {
            throw new InvalidCategoryException("Selected category does not exist.");
        }
    }

    private Product getProductByIdOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private void validateOwnership(Product product, Long sellerId) {
        if (product.getSeller() == null || !product.getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedActionException("You are not allowed to modify this product.");
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private ProductResponse mapToProductResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStock(product.getStock());
        response.setImageUrl(product.getImageUrl());

        if (product.getCategory() != null) {
            response.setCategoryId(product.getCategory().getId());
            response.setCategoryName(product.getCategory().getName());
        }

        if (product.getSeller() != null) {
            response.setSellerId(product.getSeller().getId());
            response.setSellerName(product.getSeller().getFullName());
        }

        response.setAverageRating(productReviewRepository.findAverageRatingByProductId(product.getId()));
        response.setReviewCount(productReviewRepository.countByProductId(product.getId()));

        return response;
    }
}
