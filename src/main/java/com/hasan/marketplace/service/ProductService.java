package com.hasan.marketplace.service;

import com.hasan.marketplace.dto.ProductRequest;
import com.hasan.marketplace.dto.ProductResponse;
import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request, Long sellerId);

    ProductResponse updateProduct(Long productId, ProductRequest request, Long sellerId);

    void deleteProduct(Long productId, Long sellerId);

    ProductResponse getProductById(Long productId);

    List<ProductResponse> getAllProducts();

    List<ProductResponse> getProductsBySeller(Long sellerId);

    List<ProductResponse> searchProducts(String keyword);
}

