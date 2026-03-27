package com.hasan.marketplace.service.impl;

import com.hasan.marketplace.dto.ProductRequest;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.entity.Product;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.exception.ResourceNotFoundException;
import com.hasan.marketplace.exception.UnauthorizedActionException;
import com.hasan.marketplace.repository.ProductRepository;
import com.hasan.marketplace.repository.UserRepository;
import com.hasan.marketplace.service.ProductService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public ProductResponse createProduct(ProductRequest request, Long sellerId) {
        User seller = getSellerById(sellerId);

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());
        product.setSeller(seller);

        Product savedProduct = productRepository.save(product);
        return mapToProductResponse(savedProduct);
    }

    @Override
    public ProductResponse updateProduct(Long productId, ProductRequest request, Long sellerId) {
        Product product = getProductByIdOrThrow(productId);
        validateOwnership(product, sellerId);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());

        Product updatedProduct = productRepository.save(product);
        return mapToProductResponse(updatedProduct);
    }

    @Override
    public void deleteProduct(Long productId, Long sellerId) {
        Product product = getProductByIdOrThrow(productId);
        validateOwnership(product, sellerId);
        productRepository.delete(product);
    }

    @Override
    public ProductResponse getProductById(Long productId) {
        Product product = getProductByIdOrThrow(productId);
        return mapToProductResponse(product);
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProductsBySeller(Long sellerId) {
        return productRepository.findBySellerId(sellerId)
                .stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> searchProducts(String keyword) {
        List<Product> products;

        if (keyword == null || keyword.isBlank()) {
            products = productRepository.findAll();
        } else {
            products = productRepository.findByNameContainingIgnoreCase(keyword.trim());
        }

        return products.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    private User getSellerById(Long sellerId) {
        return userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));
    }

    private Product getProductByIdOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private void validateOwnership(Product product, Long sellerId) {
        if (product.getSeller() == null || !product.getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedActionException("You are not allowed to modify this product");
        }
    }

    private ProductResponse mapToProductResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStock(product.getStock());
        response.setImageUrl(product.getImageUrl());

        if (product.getSeller() != null) {
            response.setSellerId(product.getSeller().getId());
            response.setSellerName(product.getSeller().getFullName());
        }

        return response;
    }
}

