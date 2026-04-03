package com.hasan.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hasan.marketplace.dto.ProductRequest;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.entity.Category;
import com.hasan.marketplace.entity.Product;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.repository.CategoryRepository;
import com.hasan.marketplace.exception.ResourceNotFoundException;
import com.hasan.marketplace.exception.UnauthorizedActionException;
import com.hasan.marketplace.repository.ProductRepository;
import com.hasan.marketplace.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void createProductSavesNewProductAndMapsSellerDetails() {
        ProductRequest request = buildRequest("Wireless Mouse", "Reliable mouse", new BigDecimal("39.99"), 8);
        User seller = buildSeller(1L, "Hasan Seller");
        Category category = buildCategory(1L, "Accessories");
        Product savedProduct = Product.builder()
                .id(10L)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .imageUrl(request.getImageUrl())
                .category(category)
                .seller(seller)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(request, 1L);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product persistedProduct = productCaptor.getValue();

        assertEquals("Wireless Mouse", persistedProduct.getName());
        assertEquals("Reliable mouse", persistedProduct.getDescription());
        assertEquals(new BigDecimal("39.99"), persistedProduct.getPrice());
        assertEquals(8, persistedProduct.getStock());
        assertEquals("https://cdn.example.com/image.png", persistedProduct.getImageUrl());
        assertEquals(category, persistedProduct.getCategory());
        assertEquals(seller, persistedProduct.getSeller());

        assertEquals(10L, response.getId());
        assertEquals(1L, response.getCategoryId());
        assertEquals("Accessories", response.getCategoryName());
        assertEquals(1L, response.getSellerId());
        assertEquals("Hasan Seller", response.getSellerName());
    }

    @Test
    void updateProductUpdatesOwnedProduct() {
        ProductRequest request = buildRequest("Updated Name", "Updated description", new BigDecimal("99.99"), 3);
        User seller = buildSeller(7L, "Seller Seven");
        Category category = buildCategory(1L, "Accessories");
        Product existingProduct = Product.builder()
                .id(22L)
                .name("Old Name")
                .description("Old description")
                .price(new BigDecimal("50.00"))
                .stock(11)
                .imageUrl("https://cdn.example.com/old.png")
                .seller(seller)
                .build();

        when(productRepository.findById(22L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(existingProduct)).thenReturn(existingProduct);

        ProductResponse response = productService.updateProduct(22L, request, 7L);

        verify(productRepository).save(existingProduct);
        assertEquals("Updated Name", existingProduct.getName());
        assertEquals("Updated description", existingProduct.getDescription());
        assertEquals(new BigDecimal("99.99"), existingProduct.getPrice());
        assertEquals(3, existingProduct.getStock());
        assertEquals("https://cdn.example.com/image.png", existingProduct.getImageUrl());
        assertEquals(category, existingProduct.getCategory());
        assertEquals(22L, response.getId());
        assertEquals(7L, response.getSellerId());
    }

    @Test
    void updateProductThrowsWhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.updateProduct(99L, buildRequest("Phone", "Nice", new BigDecimal("1.00"), 1), 1L)
        );

        assertEquals("Product not found with id: 99", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProductThrowsWhenSellerDoesNotOwnProduct() {
        Product product = Product.builder()
                .id(15L)
                .seller(buildSeller(2L, "Another Seller"))
                .build();
        when(productRepository.findById(15L)).thenReturn(Optional.of(product));

        UnauthorizedActionException exception = assertThrows(
                UnauthorizedActionException.class,
                () -> productService.deleteProduct(15L, 1L)
        );

        assertEquals("You are not allowed to modify this product.", exception.getMessage());
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void getProductByIdReturnsMappedResponse() {
        Product product = Product.builder()
                .id(5L)
                .name("Laptop")
                .description("Lightweight laptop")
                .price(new BigDecimal("899.99"))
                .stock(4)
                .imageUrl("https://cdn.example.com/laptop.png")
                .category(buildCategory(2L, "Laptop"))
                .seller(buildSeller(3L, "Laptop Seller"))
                .build();
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(5L);

        assertEquals("Laptop", response.getName());
        assertEquals("Lightweight laptop", response.getDescription());
        assertEquals(new BigDecimal("899.99"), response.getPrice());
        assertEquals(4, response.getStock());
        assertEquals("https://cdn.example.com/laptop.png", response.getImageUrl());
        assertEquals(2L, response.getCategoryId());
        assertEquals("Laptop", response.getCategoryName());
        assertEquals(3L, response.getSellerId());
        assertEquals("Laptop Seller", response.getSellerName());
    }

    @Test
    void searchProductsUsesFindAllWhenKeywordIsBlank() {
        when(productRepository.findAllByOrderByIdDesc()).thenReturn(List.of(
                Product.builder().id(1L).name("Mouse").seller(buildSeller(1L, "Seller")).build(),
                Product.builder().id(2L).name("Keyboard").seller(buildSeller(1L, "Seller")).build()
        ));

        List<ProductResponse> responses = productService.searchProducts("   ", null);

        verify(productRepository).findAllByOrderByIdDesc();
        verify(productRepository, never()).findByNameContainingIgnoreCaseOrderByIdDesc(any(String.class));
        assertEquals(2, responses.size());
    }

    @Test
    void searchProductsTrimsKeywordBeforeSearching() {
        Product product = Product.builder()
                .id(30L)
                .name("Phone Case")
                .seller(buildSeller(8L, "Accessories Seller"))
                .build();
        when(productRepository.findByNameContainingIgnoreCaseOrderByIdDesc("phone")).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.searchProducts("  phone  ", null);

        verify(productRepository).findByNameContainingIgnoreCaseOrderByIdDesc("phone");
        assertEquals(1, responses.size());
        assertEquals("Phone Case", responses.get(0).getName());
    }

    private ProductRequest buildRequest(String name, String description, BigDecimal price, Integer stock) {
        return ProductRequest.builder()
                .name(name)
                .description(description)
                .price(price)
                .stock(stock)
                .categoryId(1L)
                .imageUrl("https://cdn.example.com/image.png")
                .build();
    }

    private Category buildCategory(Long id, String name) {
        return Category.builder()
                .id(id)
                .name(name)
                .build();
    }

    private User buildSeller(Long id, String fullName) {
        return User.builder()
                .id(id)
                .fullName(fullName)
                .email(fullName.toLowerCase().replace(" ", ".") + "@example.com")
                .password("secret")
                .enabled(true)
                .build();
    }
}
