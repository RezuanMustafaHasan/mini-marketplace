package com.hasan.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hasan.marketplace.dto.OrderResponse;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.repository.UserRepository;
import com.hasan.marketplace.service.OrderService;
import com.hasan.marketplace.service.ProductService;
import java.util.List;
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
        List<User> users = List.of(User.builder().id(1L).email("admin@example.com").build());
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = adminService.getAllUsers();

        assertSame(users, result);
        verify(userRepository).findAll();
    }

    @Test
    void getAllProductsDelegatesToProductService() {
        List<ProductResponse> products = List.of(ProductResponse.builder().id(2L).name("Monitor").build());
        when(productService.getAllProducts()).thenReturn(products);

        List<ProductResponse> result = adminService.getAllProducts();

        assertSame(products, result);
        verify(productService).getAllProducts();
    }

    @Test
    void getAllOrdersDelegatesToOrderService() {
        List<OrderResponse> orders = List.of(OrderResponse.builder().id(3L).build());
        when(orderService.getAllOrders()).thenReturn(orders);

        List<OrderResponse> result = adminService.getAllOrders();

        assertSame(orders, result);
        verify(orderService).getAllOrders();
    }
}
