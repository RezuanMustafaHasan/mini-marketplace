package com.hasan.marketplace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.hasan.marketplace.dto.ProductRequest;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.entity.Category;
import com.hasan.marketplace.entity.Product;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.exception.UnauthorizedActionException;
import com.hasan.marketplace.repository.CategoryRepository;
import com.hasan.marketplace.repository.ProductRepository;
import com.hasan.marketplace.repository.UserRepository;
import com.hasan.marketplace.service.impl.ProductServiceImpl;
import java.math.BigDecimal;
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

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void createProductAssignsSelectedCategory() {
        User seller = User.builder().id(10L).fullName("Seller One").build();
        Category category = Category.builder().id(5L).name("Laptop").build();
        ProductRequest request = ProductRequest.builder()
                .name("Student Laptop")
                .description("Lightweight laptop")
                .price(new BigDecimal("999.99"))
                .stock(7)
                .categoryId(5L)
                .imageUrl("https://example.com/laptop.jpg")
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(seller));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(100L);
            return product;
        });

        ProductResponse response = productService.createProduct(request, 10L);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getCategoryId()).isEqualTo(5L);
        assertThat(response.getCategoryName()).isEqualTo("Laptop");
        assertThat(response.getSellerName()).isEqualTo("Seller One");
    }

    @Test
    void updateProductRejectsAnotherSeller() {
        User owner = User.builder().id(1L).fullName("Owner").build();
        Product product = Product.builder()
                .id(30L)
                .name("Phone")
                .price(new BigDecimal("450.00"))
                .stock(4)
                .seller(owner)
                .build();

        ProductRequest request = ProductRequest.builder()
                .name("Updated Phone")
                .description("Updated")
                .price(new BigDecimal("500.00"))
                .stock(3)
                .categoryId(2L)
                .build();

        when(productRepository.findById(30L)).thenReturn(Optional.of(product));

        assertThrows(UnauthorizedActionException.class, () -> productService.updateProduct(30L, request, 99L));
    }
}
