package com.hasan.marketplace.controller;

import com.hasan.marketplace.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public String showAdminDashboard() {
        return "admin-dashboard";
    }

    @GetMapping("/users")
    public String showAllUsers(Model model) {
        model.addAttribute("users", adminService.getAllUsers());
        return "admin-users";
    }

    @GetMapping("/products")
    public String showAllProducts(Model model) {
        model.addAttribute("products", adminService.getAllProducts());
        return "admin-products";
    }

    @GetMapping("/orders")
    public String showAllOrders(Model model) {
        model.addAttribute("orders", adminService.getAllOrders());
        return "admin-orders";
    }
}

