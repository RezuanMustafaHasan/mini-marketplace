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
import com.hasan.marketplace.exception.ResourceNotFoundException;
import com.hasan.marketplace.repository.CategoryRepository;
import com.hasan.marketplace.repository.ProductRepository;
import com.hasan.marketplace.repository.ProductReviewRepository;
import com.hasan.marketplace.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock
    private ProductReviewRepository productReviewRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void createProduct_shouldSaveProduct() {
        ProductRequest request = ProductRequest.builder()
                .name("Mouse")
                .description("Simple mouse")
                .price(new BigDecimal("39.99"))
                .stock(5)
                .categoryId(1L)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).fullName("Seller").build()));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.builder().id(1L).name("Accessories").build()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(10L);
            return product;
        });
        when(productReviewRepository.findAverageRatingByProductId(any())).thenReturn(null);
        when(productReviewRepository.countByProductId(any())).thenReturn(0L);

        ProductResponse response = productService.createProduct(request, 1L);

        assertEquals(10L, response.getId());
        assertEquals("Mouse", response.getName());
    }

    @Test
    void updateProduct_shouldThrowWhenProductIsMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.updateProduct(99L, ProductRequest.builder()
                        .name("Phone")
                        .price(new BigDecimal("1.00"))
                        .stock(1)
                        .categoryId(1L)
                        .build(), 1L)
        );

        assertEquals("Product not found with id: 99", exception.getMessage());
    }

    @Test
    void searchProducts_shouldReturnAllWhenKeywordIsBlank() {
        when(productRepository.findAllByOrderByIdDesc()).thenReturn(List.of(
                Product.builder().id(1L).name("Keyboard").seller(User.builder().id(1L).fullName("Seller").build()).build()
        ));
        when(productReviewRepository.findAverageRatingByProductId(any())).thenReturn(null);
        when(productReviewRepository.countByProductId(any())).thenReturn(0L);

        List<ProductResponse> products = productService.searchProducts("   ", null);

        assertEquals(1, products.size());
        verify(productRepository).findAllByOrderByIdDesc();
        verify(productRepository, never()).findByNameContainingIgnoreCaseOrderByIdDesc(any());
    }
}
