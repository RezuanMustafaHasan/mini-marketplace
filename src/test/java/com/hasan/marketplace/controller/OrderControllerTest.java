package com.hasan.marketplace.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.hasan.marketplace.dto.OrderRequest;
import com.hasan.marketplace.dto.OrderResponse;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.entity.OrderStatus;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.service.CategoryService;
import com.hasan.marketplace.service.OrderService;
import com.hasan.marketplace.service.ProductService;
import com.hasan.marketplace.service.UserService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
    void showCheckoutPageLoadsProductAndDefaultQuantity() throws Exception {
        ProductResponse product = ProductResponse.builder()
                .id(9L)
                .name("Mechanical Keyboard")
                .price(new BigDecimal("129.99"))
                .build();
        when(productService.getProductById(9L)).thenReturn(product);

        MvcResult result = mockMvc.perform(get("/checkout/9").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(model().attribute("quantity", 1))
                .andExpect(model().attributeExists("product"))
                .andReturn();

        ProductResponse modelProduct = (ProductResponse) result.getModelAndView().getModel().get("product");
        assertEquals("Mechanical Keyboard", modelProduct.getName());
    }

    @Test
    void placeOrderBuildsSingleItemOrderAndRedirects() throws Exception {
        mockMvc.perform(post("/orders")
                        .with(csrf())
                        .param("productId", "15")
                        .param("quantity", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        ArgumentCaptor<OrderRequest> requestCaptor = ArgumentCaptor.forClass(OrderRequest.class);
        verify(orderService).placeOrder(requestCaptor.capture(), org.mockito.Mockito.eq(2L));

        OrderRequest request = requestCaptor.getValue();
        assertEquals(1, request.getItems().size());
        assertEquals(15L, request.getItems().get(0).getProductId());
        assertEquals(3, request.getItems().get(0).getQuantity());
    }

    @Test
    void placeOrderUsesAuthenticatedBuyerIdWhenAvailable() throws Exception {
        when(userService.findByEmail("buyer@example.com")).thenReturn(User.builder().id(77L).build());

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "buyer@example.com",
                        "password",
                        List.of(new SimpleGrantedAuthority("ROLE_BUYER"))
                )
        );

        try {
            mockMvc.perform(post("/orders")
                            .with(csrf())
                            .param("productId", "15")
                            .param("quantity", "3"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/orders"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(orderService).placeOrder(any(OrderRequest.class), org.mockito.Mockito.eq(77L));
    }

    @Test
    void showBuyerOrdersAddsOrdersToModel() throws Exception {
        List<OrderResponse> orders = List.of(OrderResponse.builder()
                .id(21L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("59.99"))
                .build());
        when(orderService.getOrdersByBuyer(2L)).thenReturn(orders);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"));

        verify(orderService).getOrdersByBuyer(2L);
    }

    @Test
    void showOrderDetailsAddsOrderToModel() throws Exception {
        OrderResponse order = OrderResponse.builder()
                .id(44L)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("199.99"))
                .build();
        when(orderService.getOrderForBuyer(44L, 2L)).thenReturn(order);

        MvcResult result = mockMvc.perform(get("/orders/44"))
                .andExpect(status().isOk())
                .andExpect(view().name("order-details"))
                .andExpect(model().attributeExists("order"))
                .andReturn();

        OrderResponse modelOrder = (OrderResponse) result.getModelAndView().getModel().get("order");
        assertEquals(44L, modelOrder.getId());
        verify(orderService).getOrderForBuyer(44L, 2L);
    }
}
