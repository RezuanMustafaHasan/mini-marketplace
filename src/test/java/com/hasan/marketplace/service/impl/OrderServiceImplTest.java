package com.hasan.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hasan.marketplace.dto.OrderItemRequest;
import com.hasan.marketplace.dto.OrderRequest;
import com.hasan.marketplace.dto.OrderResponse;
import com.hasan.marketplace.entity.CustomerOrder;
import com.hasan.marketplace.entity.OrderItem;
import com.hasan.marketplace.entity.OrderStatus;
import com.hasan.marketplace.entity.Product;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.exception.InsufficientStockException;
import com.hasan.marketplace.repository.CustomerOrderRepository;
import com.hasan.marketplace.repository.ProductRepository;
import com.hasan.marketplace.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void placeOrder_shouldSaveOrder() {
        User buyer = User.builder().id(1L).fullName("Buyer").build();
        Product product = Product.builder()
                .id(10L)
                .name("Mouse")
                .price(new BigDecimal("20.00"))
                .stock(5)
                .seller(User.builder().id(2L).build())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder order = invocation.getArgument(0);
            order.setId(50L);
            return order;
        });

        OrderResponse response = orderService.placeOrder(OrderRequest.builder()
                .items(List.of(OrderItemRequest.builder().productId(10L).quantity(2).build()))
                .build(), 1L);

        assertEquals(50L, response.getId());
        assertEquals(new BigDecimal("40.00"), response.getTotalAmount());
        assertEquals(3, product.getStock());
        verify(productRepository).save(product);
    }

    @Test
    void placeOrder_shouldThrowWhenStockIsNotEnough() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(productRepository.findById(10L)).thenReturn(Optional.of(Product.builder()
                .id(10L)
                .name("Mouse")
                .price(new BigDecimal("20.00"))
                .stock(1)
                .seller(User.builder().id(2L).build())
                .build()));

        InsufficientStockException exception = assertThrows(
                InsufficientStockException.class,
                () -> orderService.placeOrder(OrderRequest.builder()
                        .items(List.of(OrderItemRequest.builder().productId(10L).quantity(2).build()))
                        .build(), 1L)
        );

        assertEquals("Insufficient stock for product: Mouse", exception.getMessage());
    }

    @Test
    void getOrdersByBuyer_shouldReturnMappedOrders() {
        User buyer = User.builder().id(1L).fullName("Buyer").build();
        Product product = Product.builder()
                .id(10L)
                .name("Keyboard")
                .price(new BigDecimal("50.00"))
                .seller(User.builder().id(2L).build())
                .build();

        CustomerOrder order = CustomerOrder.builder()
                .id(7L)
                .buyer(buyer)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .build();
        order.addOrderItem(OrderItem.builder()
                .product(product)
                .quantity(2)
                .priceAtPurchase(new BigDecimal("50.00"))
                .build());

        when(customerOrderRepository.findByBuyerIdOrderByOrderDateDesc(1L)).thenReturn(List.of(order));

        List<OrderResponse> orders = orderService.getOrdersByBuyer(1L);

        assertEquals(1, orders.size());
        assertEquals(7L, orders.get(0).getId());
        assertEquals("Keyboard", orders.get(0).getItems().get(0).getProductName());
    }
}
