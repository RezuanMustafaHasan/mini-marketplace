package com.hasan.marketplace.service.impl;

import com.hasan.marketplace.dto.OrderResponse;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.repository.UserRepository;
import com.hasan.marketplace.service.AdminService;
import com.hasan.marketplace.service.OrderService;
import com.hasan.marketplace.service.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ProductService productService;
    private final OrderService orderService;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }
}

