package com.hasan.marketplace.service.impl;

import com.hasan.marketplace.dto.OrderItemRequest;
import com.hasan.marketplace.dto.OrderItemResponse;
import com.hasan.marketplace.dto.OrderRequest;
import com.hasan.marketplace.dto.OrderResponse;
import com.hasan.marketplace.entity.CustomerOrder;
import com.hasan.marketplace.entity.OrderItem;
import com.hasan.marketplace.entity.OrderStatus;
import com.hasan.marketplace.entity.Product;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.exception.InsufficientStockException;
import com.hasan.marketplace.exception.InvalidOrderStateTransitionException;
import com.hasan.marketplace.exception.ResourceNotFoundException;
import com.hasan.marketplace.exception.UnauthorizedActionException;
import com.hasan.marketplace.repository.CustomerOrderRepository;
import com.hasan.marketplace.repository.ProductRepository;
import com.hasan.marketplace.repository.UserRepository;
import com.hasan.marketplace.service.OrderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OrderResponse placeOrder(OrderRequest request, Long buyerId) {
        User buyer = getBuyerById(buyerId);

        CustomerOrder order = new CustomerOrder();
        order.setBuyer(buyer);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = getProductById(itemRequest.getProductId());

            if (itemRequest.getQuantity() > product.getStock()) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
            }

            BigDecimal priceAtPurchase = product.getPrice();
            BigDecimal lineTotal = priceAtPurchase.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPriceAtPurchase(priceAtPurchase);

            order.addOrderItem(orderItem);

            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product);

            totalAmount = totalAmount.add(lineTotal);
        }

        order.setTotalAmount(totalAmount);

        CustomerOrder savedOrder = customerOrderRepository.save(order);
        return mapToOrderResponse(savedOrder, null);
    }

    @Override
    public List<OrderResponse> getOrdersByBuyer(Long buyerId) {
        return customerOrderRepository.findByBuyerIdOrderByOrderDateDesc(buyerId)
                .stream()
                .map(order -> mapToOrderResponse(order, null))
                .toList();
    }

    @Override
    public OrderResponse getOrderForBuyer(Long orderId, Long buyerId) {
        CustomerOrder order = getOrderByIdOrThrow(orderId);
        validateBuyerOwnership(order, buyerId);
        return mapToOrderResponse(order, null);
    }

    @Override
    public OrderResponse getOrderById(Long orderId) {
        return mapToOrderResponse(getOrderByIdOrThrow(orderId), null);
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return customerOrderRepository.findAll()
                .stream()
                .map(order -> mapToOrderResponse(order, null))
                .toList();
    }

    @Override
    public List<OrderResponse> getOrdersForSeller(Long sellerId) {
        return customerOrderRepository.findDistinctByItemsProductSellerIdOrderByOrderDateDesc(sellerId)
                .stream()
                .map(order -> mapToOrderResponse(order, sellerId))
                .toList();
    }

    @Override
    public OrderResponse getOrderForSeller(Long orderId, Long sellerId) {
        CustomerOrder order = getOrderByIdOrThrow(orderId);
        validateSellerAccess(order, sellerId);
        return mapToOrderResponse(order, sellerId);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatusBySeller(Long orderId, Long sellerId, OrderStatus newStatus) {
        CustomerOrder order = getOrderByIdOrThrow(orderId);
        validateSellerAccess(order, sellerId);
        validateSellerStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        CustomerOrder updatedOrder = customerOrderRepository.save(order);
        return mapToOrderResponse(updatedOrder, sellerId);
    }

    @Override
    public long countOrdersForSeller(Long sellerId) {
        return customerOrderRepository.countDistinctByItemsProductSellerId(sellerId);
    }

    private User getBuyerById(Long buyerId) {
        return userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found with id: " + buyerId));
    }

    private Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private CustomerOrder getOrderByIdOrThrow(Long orderId) {
        return customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private void validateBuyerOwnership(CustomerOrder order, Long buyerId) {
        if (order.getBuyer() == null || !buyerId.equals(order.getBuyer().getId())) {
            throw new UnauthorizedActionException("You can only view your own orders.");
        }
    }

    private void validateSellerAccess(CustomerOrder order, Long sellerId) {
        boolean ownsAtLeastOneItem = order.getItems()
                .stream()
                .anyMatch(item -> item.getProduct() != null
                        && item.getProduct().getSeller() != null
                        && sellerId.equals(item.getProduct().getSeller().getId()));

        if (!ownsAtLeastOneItem) {
            throw new UnauthorizedActionException("You are not allowed to manage this order.");
        }
    }

    private void validateSellerStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (newStatus == null) {
            throw new InvalidOrderStateTransitionException("Please select a valid order status.");
        }

        if (!currentStatus.canSellerTransitionTo(newStatus)) {
            throw new InvalidOrderStateTransitionException(
                    "Seller cannot change order status from "
                            + currentStatus.getDisplayName()
                            + " to "
                            + newStatus.getDisplayName()
                            + "."
            );
        }
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
        BigDecimal lineTotal = orderItem.getPriceAtPurchase()
                .multiply(BigDecimal.valueOf(orderItem.getQuantity()));

        OrderItemResponse response = new OrderItemResponse();
        response.setProductId(orderItem.getProduct().getId());
        response.setProductName(orderItem.getProduct().getName());
        response.setQuantity(orderItem.getQuantity());
        response.setPriceAtPurchase(orderItem.getPriceAtPurchase());
        response.setLineTotal(lineTotal);
        return response;
    }

    private OrderResponse mapToOrderResponse(CustomerOrder order, Long sellerId) {
        List<OrderItem> orderItems = order.getItems();

        if (sellerId != null) {
            orderItems = orderItems.stream()
                    .filter(item -> item.getProduct() != null
                            && item.getProduct().getSeller() != null
                            && sellerId.equals(item.getProduct().getSeller().getId()))
                    .toList();
        }

        List<OrderItemResponse> items = orderItems.stream()
                .map(this::mapToOrderItemResponse)
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(OrderItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setBuyerId(order.getBuyer().getId());
        response.setBuyerName(order.getBuyer().getFullName());
        response.setOrderDate(order.getOrderDate());
        response.setStatus(order.getStatus());
        response.setTotalAmount(sellerId == null ? order.getTotalAmount() : totalAmount);
        response.setItems(items);
        return response;
    }
}
