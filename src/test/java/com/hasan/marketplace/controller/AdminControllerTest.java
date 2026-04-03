package com.hasan.marketplace.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.hasan.marketplace.dto.OrderResponse;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.dto.UserResponse;
import com.hasan.marketplace.entity.OrderStatus;
import com.hasan.marketplace.service.AdminService;
import com.hasan.marketplace.service.CategoryService;
import com.hasan.marketplace.service.UserService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private UserService userService;

    @Test
    void showAdminDashboardReturnsDashboardView() throws Exception {
        mockMvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"));
    }

    @Test
    void showAllUsersAddsUsersToModel() throws Exception {
        when(adminService.getAllUsers()).thenReturn(List.of(
                UserResponse.builder()
                        .id(1L)
                        .fullName("Admin User")
                        .email("admin@example.com")
                        .enabled(true)
                        .role("ADMIN")
                        .build()
        ));

        mockMvc.perform(get("/admin/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-users"))
                .andExpect(model().attributeExists("users"));

        verify(adminService).getAllUsers();
    }

    @Test
    void showAllProductsAddsProductsToModel() throws Exception {
        when(adminService.getAllProducts()).thenReturn(List.of(
                ProductResponse.builder().id(5L).name("Monitor").price(new BigDecimal("249.99")).build()
        ));

        mockMvc.perform(get("/admin/products").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-products"))
                .andExpect(model().attributeExists("products"));

        verify(adminService).getAllProducts();
    }

    @Test
    void showAllOrdersAddsOrdersToModel() throws Exception {
        when(adminService.getAllOrders()).thenReturn(List.of(
                OrderResponse.builder().id(9L).status(OrderStatus.PENDING).totalAmount(new BigDecimal("59.99")).build()
        ));

        mockMvc.perform(get("/admin/orders").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-orders"))
                .andExpect(model().attributeExists("orders"));

        verify(adminService).getAllOrders();
    }
}
