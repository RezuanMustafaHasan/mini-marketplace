package com.hasan.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hasan.marketplace.dto.OrderResponse;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.dto.UserResponse;
import com.hasan.marketplace.entity.Role;
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.repository.UserRepository;
import com.hasan.marketplace.service.OrderService;
import com.hasan.marketplace.service.ProductService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductService productService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void getAllUsersDelegatesToUserRepository() {
        List<User> users = List.of(
                User.builder()
                        .id(1L)
                        .fullName("Admin User")
                        .email("admin@example.com")
                        .enabled(true)
                        .roles(Set.of(Role.builder().name(RoleName.ADMIN).build()))
                        .build()
        );
        when(userRepository.findAll()).thenReturn(users);

        List<UserResponse> result = adminService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Admin User", result.get(0).getFullName());
        assertEquals("admin@example.com", result.get(0).getEmail());
        assertEquals("ADMIN", result.get(0).getRole());
        verify(userRepository).findAll();
    }

    @Test
    void getAllProductsDelegatesToProductService() {
        List<ProductResponse> products = List.of(ProductResponse.builder().id(2L).name("Monitor").build());
        when(productService.getAllProducts()).thenReturn(products);

        List<ProductResponse> result = adminService.getAllProducts();

        assertEquals(products, result);
        verify(productService).getAllProducts();
    }

    @Test
    void getAllOrdersDelegatesToOrderService() {
        List<OrderResponse> orders = List.of(OrderResponse.builder().id(3L).build());
        when(orderService.getAllOrders()).thenReturn(orders);

        List<OrderResponse> result = adminService.getAllOrders();

        assertEquals(orders, result);
        verify(orderService).getAllOrders();
    }
}
