package com.hasan.marketplace.controller;

import com.hasan.marketplace.dto.SellerDashboardSummaryResponse;
import com.hasan.marketplace.entity.OrderStatus;
import com.hasan.marketplace.service.OrderService;
import com.hasan.marketplace.service.ProductService;
import com.hasan.marketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/seller")
public class SellerController {

    private final ProductService productService;
    private final OrderService orderService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String showSellerDashboard(Model model) {
        Long sellerId = getCurrentSellerId();

        SellerDashboardSummaryResponse summary = SellerDashboardSummaryResponse.builder()
                .totalProducts(productService.countProductsBySeller(sellerId))
                .totalOrders(orderService.countOrdersForSeller(sellerId))
                .build();

        model.addAttribute("summary", summary);
        return "seller-dashboard";
    }

    @GetMapping("/orders")
    public String showSellerOrders(Model model) {
        model.addAttribute("orders", orderService.getOrdersForSeller(getCurrentSellerId()));
        return "seller-orders";
    }

    @GetMapping("/orders/{id}")
    public String showSellerOrderDetails(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getOrderForSeller(id, getCurrentSellerId()));
        return "seller-order-details";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        orderService.updateOrderStatusBySeller(id, getCurrentSellerId(), status);
        return "redirect:/seller/orders/" + id;
    }

    private Long getCurrentSellerId() {
        return userService.getAuthenticatedUser().getId();
    }
}
