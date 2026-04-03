package com.hasan.marketplace.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.hasan.marketplace.service.OrderService;
import com.hasan.marketplace.service.ProductService;
import com.hasan.marketplace.service.UserService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void checkoutPage_shouldShowProduct() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(java.util.List.of());
        when(productService.getProductById(9L)).thenReturn(ProductResponse.builder()
                .id(9L)
                .name("Keyboard")
                .price(new BigDecimal("129.99"))
                .build());

        mockMvc.perform(get("/checkout/9").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(model().attributeExists("product"));
    }

    @Test
    void placeOrder_shouldRedirectToOrdersPage() throws Exception {
        mockMvc.perform(post("/orders")
                        .with(user("buyer@example.com").roles("BUYER"))
                        .with(csrf())
                        .param("productId", "15")
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        verify(orderService).placeOrder(any(), org.mockito.Mockito.eq(2L));
    }
}
