package com.hasan.marketplace.controller;

import com.hasan.marketplace.dto.OrderItemRequest;
import com.hasan.marketplace.dto.OrderRequest;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.entity.RoleName;
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
        User currentUser = getCurrentUser();

        if (currentUser != null && hasRole(currentUser, RoleName.ADMIN)) {
            model.addAttribute("orders", orderService.getAllOrders());
            model.addAttribute("pageTitle", "All Orders");
            model.addAttribute("backUrl", "/admin");
            return "orders";
        }

        model.addAttribute("orders", orderService.getOrdersByBuyer(getCurrentUserId()));
        model.addAttribute("pageTitle", "My Orders");
        model.addAttribute("backUrl", "/products");
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String showOrderDetails(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();

        if (currentUser != null && hasRole(currentUser, RoleName.ADMIN)) {
            model.addAttribute("order", orderService.getOrderById(id));
            model.addAttribute("backUrl", "/admin/orders");
            return "order-details";
        }

        model.addAttribute("order", orderService.getOrderForBuyer(id, getCurrentUserId()));
        model.addAttribute("backUrl", "/orders");
        return "order-details";
    }

    private Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null && user.getId() != null ? user.getId() : TEMP_BUYER_ID;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserService userService = userServiceProvider.getIfAvailable();

        if (authentication == null || userService == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();
        if (email == null || "anonymousUser".equals(email)) {
            return null;
        }

        try {
            return userService.findByEmail(email);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean hasRole(User user, RoleName roleName) {
        UserService userService = userServiceProvider.getIfAvailable();
        return userService != null && userService.hasRole(user, roleName);
    }
}
