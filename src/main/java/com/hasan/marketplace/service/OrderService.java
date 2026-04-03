package com.hasan.marketplace.service;

import com.hasan.marketplace.dto.OrderRequest;
import com.hasan.marketplace.dto.OrderResponse;
import com.hasan.marketplace.entity.OrderStatus;
import java.util.List;

public interface OrderService {

    OrderResponse placeOrder(OrderRequest request, Long buyerId);

    List<OrderResponse> getOrdersByBuyer(Long buyerId);

    OrderResponse getOrderForBuyer(Long orderId, Long buyerId);

    OrderResponse getOrderById(Long orderId);

    List<OrderResponse> getAllOrders();

    List<OrderResponse> getOrdersForSeller(Long sellerId);

    OrderResponse getOrderForSeller(Long orderId, Long sellerId);

    OrderResponse updateOrderStatusBySeller(Long orderId, Long sellerId, OrderStatus newStatus);

    long countOrdersForSeller(Long sellerId);
}
