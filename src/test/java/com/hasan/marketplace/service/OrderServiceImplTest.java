package com.hasan.marketplace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.hasan.marketplace.dto.OrderResponse;
import com.hasan.marketplace.entity.Category;
import com.hasan.marketplace.entity.CustomerOrder;
import com.hasan.marketplace.entity.OrderItem;
import com.hasan.marketplace.entity.OrderStatus;
import com.hasan.marketplace.entity.Product;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.exception.InvalidOrderStateTransitionException;
import com.hasan.marketplace.repository.CustomerOrderRepository;
import com.hasan.marketplace.repository.ProductRepository;
import com.hasan.marketplace.repository.UserRepository;
import com.hasan.marketplace.service.impl.OrderServiceImpl;
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
    void getOrdersForSellerFiltersOnlySellerItems() {
        User buyer = User.builder().id(1L).fullName("Buyer").build();
        User sellerOne = User.builder().id(10L).fullName("Seller One").build();
        User sellerTwo = User.builder().id(20L).fullName("Seller Two").build();
        Category category = Category.builder().id(1L).name("Phones").build();

        Product sellerProduct = Product.builder()
                .id(100L)
                .name("Seller Phone")
                .price(new BigDecimal("200.00"))
                .seller(sellerOne)
                .category(category)
                .build();

        Product otherSellerProduct = Product.builder()
                .id(200L)
                .name("Other Tablet")
                .price(new BigDecimal("300.00"))
                .seller(sellerTwo)
                .category(category)
                .build();

        OrderItem sellerItem = OrderItem.builder()
                .product(sellerProduct)
                .quantity(2)
                .priceAtPurchase(new BigDecimal("200.00"))
                .build();

        OrderItem otherItem = OrderItem.builder()
                .product(otherSellerProduct)
                .quantity(1)
                .priceAtPurchase(new BigDecimal("300.00"))
                .build();

        CustomerOrder order = CustomerOrder.builder()
                .id(500L)
                .buyer(buyer)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("700.00"))
                .items(List.of(sellerItem, otherItem))
                .build();

        when(customerOrderRepository.findDistinctByItemsProductSellerIdOrderByOrderDateDesc(10L))
                .thenReturn(List.of(order));

        List<OrderResponse> orders = orderService.getOrdersForSeller(10L);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getItems()).hasSize(1);
        assertThat(orders.get(0).getItems().get(0).getProductName()).isEqualTo("Seller Phone");
        assertThat(orders.get(0).getTotalAmount()).isEqualByComparingTo("400.00");
    }

    @Test
    void updateOrderStatusBySellerMovesOrderForward() {
        CustomerOrder order = buildSellerOrder(OrderStatus.PENDING, 10L);

        when(customerOrderRepository.findById(900L)).thenReturn(Optional.of(order));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatusBySeller(900L, 10L, OrderStatus.CONFIRMED);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void updateOrderStatusBySellerRejectsInvalidDeliveryConfirmation() {
        CustomerOrder order = buildSellerOrder(OrderStatus.PENDING, 10L);

        when(customerOrderRepository.findById(900L)).thenReturn(Optional.of(order));

        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> orderService.updateOrderStatusBySeller(900L, 10L, OrderStatus.DELIVERED)
        );
    }

    private CustomerOrder buildSellerOrder(OrderStatus status, Long sellerId) {
        User buyer = User.builder().id(1L).fullName("Buyer").build();
        User seller = User.builder().id(sellerId).fullName("Seller").build();
        Category category = Category.builder().id(2L).name("Accessories").build();
        Product product = Product.builder()
                .id(40L)
                .name("Wireless Mouse")
                .seller(seller)
                .category(category)
                .price(new BigDecimal("50.00"))
                .build();

        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(2)
                .priceAtPurchase(new BigDecimal("50.00"))
                .build();

        return CustomerOrder.builder()
                .id(900L)
                .buyer(buyer)
                .orderDate(LocalDateTime.now())
                .status(status)
                .totalAmount(new BigDecimal("100.00"))
                .items(List.of(item))
                .build();
    }
}
