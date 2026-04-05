package com.hasan.marketplace.controller.api;

import com.hasan.marketplace.dto.OrderRequest;
import com.hasan.marketplace.dto.OrderResponse;
import com.hasan.marketplace.entity.OrderStatus;
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.exception.UnauthorizedActionException;
import com.hasan.marketplace.service.OrderService;
import com.hasan.marketplace.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService orderService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        User currentUser = userService.getAuthenticatedUser();

        if (userService.hasRole(currentUser, RoleName.SELLER)) {
            throw new UnauthorizedActionException("Sellers cannot place buyer orders.");
        }

        OrderResponse created = orderService.placeOrder(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<OrderResponse> getOrders() {
        User currentUser = userService.getAuthenticatedUser();

        if (userService.hasRole(currentUser, RoleName.ADMIN)) {
            return orderService.getAllOrders();
        }

        if (userService.hasRole(currentUser, RoleName.SELLER)) {
            return orderService.getOrdersForSeller(currentUser.getId());
        }

        return orderService.getOrdersByBuyer(currentUser.getId());
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable Long id) {
        User currentUser = userService.getAuthenticatedUser();

        if (userService.hasRole(currentUser, RoleName.ADMIN)) {
            return orderService.getOrderById(id);
        }

        if (userService.hasRole(currentUser, RoleName.SELLER)) {
            return orderService.getOrderForSeller(id, currentUser.getId());
        }

        return orderService.getOrderForBuyer(id, currentUser.getId());
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateOrderStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        User currentUser = userService.getAuthenticatedUser();

        if (!userService.hasRole(currentUser, RoleName.SELLER)) {
            throw new UnauthorizedActionException("Only sellers can update order status.");
        }

        return orderService.updateOrderStatusBySeller(id, currentUser.getId(), status);
    }
}
