package com.hasan.marketplace.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.service.CategoryService;
import com.hasan.marketplace.service.ProductService;
import com.hasan.marketplace.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private UserService userService;

    @Test
    void productsPage_shouldShowProducts() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of());
        when(productService.searchProducts(null, null)).thenReturn(List.of(
                ProductResponse.builder().id(1L).name("Mouse").build()
        ));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attributeExists("products"));
    }

    @Test
    void createProduct_shouldReturnFormWhenInputIsInvalid() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(post("/seller/products")
                        .with(user("seller@example.com").roles("SELLER"))
                        .with(csrf())
                        .param("name", "")
                        .param("price", "")
                        .param("stock", "2")
                        .param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-form"))
                .andExpect(model().attributeHasFieldErrors("product", "name", "price"));

        verify(productService, never()).createProduct(any(), any());
    }

    @Test
    void createProduct_shouldRedirectWhenSuccessful() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(post("/seller/products")
                        .with(user("seller@example.com").roles("SELLER"))
                        .with(csrf())
                        .param("name", "Keyboard")
                        .param("description", "Simple keyboard")
                        .param("price", "99.99")
                        .param("stock", "5")
                        .param("categoryId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seller/products"));

        verify(productService).createProduct(any(), org.mockito.Mockito.eq(1L));
    }
}
