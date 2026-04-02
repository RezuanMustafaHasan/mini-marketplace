package com.hasan.marketplace.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.hasan.marketplace.dto.ProductRequest;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void showAllProductsUsesKeywordSearchAndReturnsProductsView() throws Exception {
        when(productService.searchProducts("mouse")).thenReturn(List.of(
                ProductResponse.builder().id(1L).name("Mouse").build()
        ));

        mockMvc.perform(get("/products")
                        .with(user("seller"))
                        .param("keyword", "mouse"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attribute("keyword", "mouse"))
                .andExpect(model().attributeExists("products"));

        verify(productService).searchProducts("mouse");
    }

    @Test
    void createProductWithInvalidInputReturnsFormWithoutCallingService() throws Exception {
        mockMvc.perform(post("/seller/products")
                        .with(user("seller"))
                        .with(csrf())
                        .param("name", "")
                        .param("description", "Missing required price")
                        .param("stock", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-form"))
                .andExpect(model().attribute("formTitle", "Add Product"))
                .andExpect(model().attribute("formAction", "/seller/products"))
                .andExpect(model().attributeHasFieldErrors("product", "name", "price"));

        verify(productService, never()).createProduct(any(ProductRequest.class), any(Long.class));
    }

    @Test
    void createProductWithValidInputRedirectsToSellerProducts() throws Exception {
        mockMvc.perform(post("/seller/products")
                        .with(user("seller"))
                        .with(csrf())
                        .param("name", "Mechanical Keyboard")
                        .param("description", "RGB keyboard")
                        .param("price", "129.99")
                        .param("stock", "12")
                        .param("imageUrl", "https://cdn.example.com/keyboard.png"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seller/products"));

        ArgumentCaptor<ProductRequest> requestCaptor = ArgumentCaptor.forClass(ProductRequest.class);
        verify(productService).createProduct(requestCaptor.capture(), org.mockito.Mockito.eq(1L));

        ProductRequest capturedRequest = requestCaptor.getValue();
        assertEquals("Mechanical Keyboard", capturedRequest.getName());
        assertEquals("RGB keyboard", capturedRequest.getDescription());
        assertEquals(new BigDecimal("129.99"), capturedRequest.getPrice());
        assertEquals(12, capturedRequest.getStock());
        assertEquals("https://cdn.example.com/keyboard.png", capturedRequest.getImageUrl());
    }

    @Test
    void showEditFormPrepopulatesProductRequestModel() throws Exception {
        when(productService.getProductById(3L)).thenReturn(ProductResponse.builder()
                .id(3L)
                .name("Desk Lamp")
                .description("Warm light")
                .price(new BigDecimal("24.99"))
                .stock(6)
                .imageUrl("https://cdn.example.com/lamp.png")
                .build());

        MvcResult result = mockMvc.perform(get("/seller/products/edit/3").with(user("seller")))
                .andExpect(status().isOk())
                .andExpect(view().name("product-form"))
                .andExpect(model().attribute("productId", 3L))
                .andExpect(model().attribute("formTitle", "Edit Product"))
                .andExpect(model().attribute("formAction", "/seller/products/update/3"))
                .andReturn();

        ProductRequest product = (ProductRequest) result.getModelAndView().getModel().get("product");
        assertEquals("Desk Lamp", product.getName());
        assertEquals("Warm light", product.getDescription());
        assertEquals(new BigDecimal("24.99"), product.getPrice());
        assertEquals(6, product.getStock());
        assertEquals("https://cdn.example.com/lamp.png", product.getImageUrl());
    }

    @Test
    void deleteProductRedirectsToSellerProducts() throws Exception {
        mockMvc.perform(post("/seller/products/delete/9")
                        .with(user("seller"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seller/products"));

        verify(productService).deleteProduct(9L, 1L);
    }
}
