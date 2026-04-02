package com.hasan.marketplace.service;

import com.hasan.marketplace.dto.OrderRequest;
import com.hasan.marketplace.dto.OrderResponse;
import java.util.List;

public interface OrderService {

    OrderResponse placeOrder(OrderRequest request, Long buyerId);

    List<OrderResponse> getOrdersByBuyer(Long buyerId);

    OrderResponse getOrderById(Long orderId);

    List<OrderResponse> getAllOrders();

    List<OrderResponse> getOrdersForSeller(Long sellerId);
}

