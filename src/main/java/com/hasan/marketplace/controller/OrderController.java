package com.hasan.marketplace.controller;

import com.hasan.marketplace.dto.OrderItemRequest;
import com.hasan.marketplace.dto.OrderRequest;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.service.OrderService;
import com.hasan.marketplace.service.ProductService;
import com.hasan.marketplace.service.UserService;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class OrderController {

    private static final Long TEMP_BUYER_ID = 2L;

    private final ProductService productService;
    private final OrderService orderService;
    private final ObjectProvider<UserService> userServiceProvider;

    @GetMapping("/checkout/{productId}")
    public String showCheckoutPage(@PathVariable Long productId, Model model) {
        ProductResponse product = productService.getProductById(productId);
        model.addAttribute("product", product);
        model.addAttribute("quantity", 1);
        return "checkout";
    }

    @PostMapping("/orders")
    public String placeOrder(@RequestParam Long productId, @RequestParam Integer quantity) {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(productId);
        itemRequest.setQuantity(quantity);

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItems(Collections.singletonList(itemRequest));

        orderService.placeOrder(orderRequest, getCurrentUserId());
        return "redirect:/orders";
    }

    @GetMapping("/orders")
    public String showBuyerOrders(Model model) {
        model.addAttribute("orders", orderService.getOrdersByBuyer(getCurrentUserId()));
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String showOrderDetails(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getOrderById(id));
        return "order-details";
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserService userService = userServiceProvider.getIfAvailable();

        if (authentication == null || userService == null || !authentication.isAuthenticated()) {
            return TEMP_BUYER_ID;
        }

        String email = authentication.getName();
        if (email == null || "anonymousUser".equals(email)) {
            return TEMP_BUYER_ID;
        }

        User user = userService.findByEmail(email);
        return user != null && user.getId() != null ? user.getId() : TEMP_BUYER_ID;
    }
}
