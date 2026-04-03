package com.hasan.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hasan.marketplace.dto.OrderItemRequest;
import com.hasan.marketplace.dto.OrderResponse;
import com.hasan.marketplace.dto.OrderRequest;
import com.hasan.marketplace.entity.CustomerOrder;
import com.hasan.marketplace.entity.OrderItem;
import com.hasan.marketplace.entity.OrderStatus;
import com.hasan.marketplace.entity.Product;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.exception.InsufficientStockException;
import com.hasan.marketplace.exception.ResourceNotFoundException;
import com.hasan.marketplace.repository.CustomerOrderRepository;
import com.hasan.marketplace.repository.ProductRepository;
import com.hasan.marketplace.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    void placeOrderSavesOrderUpdatesStockAndMapsResponse() {
        User buyer = buildUser(1L, "Buyer One");
        User sellerOne = buildUser(2L, "Seller One");
        User sellerTwo = buildUser(3L, "Seller Two");
        Product mouse = buildProduct(10L, "Wireless Mouse", new BigDecimal("39.99"), 8, sellerOne);
        Product keyboard = buildProduct(11L, "Keyboard", new BigDecimal("59.99"), 4, sellerTwo);
        OrderRequest request = OrderRequest.builder()
                .items(List.of(
                        buildOrderItemRequest(10L, 2),
                        buildOrderItemRequest(11L, 1)
                ))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(mouse));
        when(productRepository.findById(11L)).thenReturn(Optional.of(keyboard));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder order = invocation.getArgument(0);
            order.setId(55L);
            return order;
        });

        OrderResponse response = orderService.placeOrder(request, 1L);

        ArgumentCaptor<CustomerOrder> orderCaptor = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(customerOrderRepository).save(orderCaptor.capture());
        CustomerOrder persistedOrder = orderCaptor.getValue();

        assertSame(buyer, persistedOrder.getBuyer());
        assertEquals(OrderStatus.PENDING, persistedOrder.getStatus());
        assertNotNull(persistedOrder.getOrderDate());
        assertEquals(new BigDecimal("139.97"), persistedOrder.getTotalAmount());
        assertEquals(2, persistedOrder.getItems().size());
        assertEquals(6, mouse.getStock());
        assertEquals(3, keyboard.getStock());
        verify(productRepository).save(mouse);
        verify(productRepository).save(keyboard);

        assertEquals(55L, response.getId());
        assertEquals(1L, response.getBuyerId());
        assertEquals("Buyer One", response.getBuyerName());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("139.97"), response.getTotalAmount());
        assertEquals(2, response.getItems().size());
        assertEquals(10L, response.getItems().get(0).getProductId());
        assertEquals("Wireless Mouse", response.getItems().get(0).getProductName());
        assertEquals(new BigDecimal("79.98"), response.getItems().get(0).getLineTotal());
        assertEquals(11L, response.getItems().get(1).getProductId());
        assertEquals(new BigDecimal("59.99"), response.getItems().get(1).getLineTotal());
    }

    @Test
    void placeOrderThrowsWhenBuyerDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.placeOrder(
                        OrderRequest.builder().items(List.of(buildOrderItemRequest(10L, 1))).build(),
                        99L
                )
        );

        assertEquals("Buyer not found with id: 99", exception.getMessage());
        verifyNoInteractions(productRepository, customerOrderRepository);
    }

    @Test
    void placeOrderThrowsWhenProductDoesNotExist() {
        User buyer = buildUser(1L, "Buyer One");
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(productRepository.findById(88L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.placeOrder(
                        OrderRequest.builder().items(List.of(buildOrderItemRequest(88L, 1))).build(),
                        1L
                )
        );

        assertEquals("Product not found with id: 88", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
        verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
    }

    @Test
    void placeOrderThrowsWhenStockIsInsufficient() {
        User buyer = buildUser(1L, "Buyer One");
        Product product = buildProduct(20L, "Gaming Headset", new BigDecimal("99.99"), 1, buildUser(2L, "Seller"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product));

        InsufficientStockException exception = assertThrows(
                InsufficientStockException.class,
                () -> orderService.placeOrder(
                        OrderRequest.builder().items(List.of(buildOrderItemRequest(20L, 2))).build(),
                        1L
                )
        );

        assertEquals("Insufficient stock for product: Gaming Headset", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
        verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
        assertEquals(1, product.getStock());
    }

    @Test
    void getOrdersByBuyerReturnsMappedOrders() {
        User buyer = buildUser(5L, "Buyer Five");
        Product product = buildProduct(30L, "Desk Lamp", new BigDecimal("24.99"), 9, buildUser(8L, "Seller"));
        CustomerOrder order = buildOrder(
                70L,
                buyer,
                LocalDateTime.of(2026, 4, 2, 10, 30),
                OrderStatus.CONFIRMED,
                new BigDecimal("49.98"),
                buildOrderItem(product, 2, new BigDecimal("24.99"))
        );
        when(customerOrderRepository.findByBuyerIdOrderByOrderDateDesc(5L)).thenReturn(List.of(order));

        List<OrderResponse> responses = orderService.getOrdersByBuyer(5L);

        assertEquals(1, responses.size());
        assertEquals(70L, responses.get(0).getId());
        assertEquals(5L, responses.get(0).getBuyerId());
        assertEquals("Buyer Five", responses.get(0).getBuyerName());
        assertEquals(OrderStatus.CONFIRMED, responses.get(0).getStatus());
        assertEquals(new BigDecimal("49.98"), responses.get(0).getTotalAmount());
        assertEquals(1, responses.get(0).getItems().size());
        assertEquals("Desk Lamp", responses.get(0).getItems().get(0).getProductName());
        assertEquals(new BigDecimal("49.98"), responses.get(0).getItems().get(0).getLineTotal());
    }

    @Test
    void getOrderByIdReturnsMappedOrder() {
        User buyer = buildUser(6L, "Buyer Six");
        Product product = buildProduct(31L, "Standing Desk", new BigDecimal("249.99"), 3, buildUser(12L, "Seller"));
        LocalDateTime orderDate = LocalDateTime.of(2026, 4, 3, 14, 45);
        CustomerOrder order = buildOrder(
                71L,
                buyer,
                orderDate,
                OrderStatus.CONFIRMED,
                new BigDecimal("249.99"),
                buildOrderItem(product, 1, new BigDecimal("249.99"))
        );
        when(customerOrderRepository.findById(71L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(71L);

        assertEquals(71L, response.getId());
        assertEquals(6L, response.getBuyerId());
        assertEquals("Buyer Six", response.getBuyerName());
        assertEquals(orderDate, response.getOrderDate());
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        assertEquals(new BigDecimal("249.99"), response.getTotalAmount());
        assertEquals(1, response.getItems().size());
        assertEquals(31L, response.getItems().get(0).getProductId());
        assertEquals("Standing Desk", response.getItems().get(0).getProductName());
        assertEquals(1, response.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("249.99"), response.getItems().get(0).getPriceAtPurchase());
        assertEquals(new BigDecimal("249.99"), response.getItems().get(0).getLineTotal());
    }

    @Test
    void getOrderByIdThrowsWhenOrderDoesNotExist() {
        when(customerOrderRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getOrderById(404L)
        );

        assertEquals("Order not found with id: 404", exception.getMessage());
    }

    @Test
    void getAllOrdersReturnsMappedOrders() {
        User buyerOne = buildUser(7L, "Buyer Seven");
        User buyerTwo = buildUser(8L, "Buyer Eight");
        Product productOne = buildProduct(32L, "Laptop Stand", new BigDecimal("34.99"), 7, buildUser(13L, "Seller One"));
        Product productTwo = buildProduct(33L, "USB Hub", new BigDecimal("19.99"), 10, buildUser(14L, "Seller Two"));
        CustomerOrder firstOrder = buildOrder(
                72L,
                buyerOne,
                LocalDateTime.of(2026, 4, 1, 16, 15),
                OrderStatus.PENDING,
                new BigDecimal("34.99"),
                buildOrderItem(productOne, 1, new BigDecimal("34.99"))
        );
        CustomerOrder secondOrder = buildOrder(
                73L,
                buyerTwo,
                LocalDateTime.of(2026, 4, 2, 8, 20),
                OrderStatus.CANCELLED,
                new BigDecimal("39.98"),
                buildOrderItem(productTwo, 2, new BigDecimal("19.99"))
        );
        when(customerOrderRepository.findAll()).thenReturn(List.of(firstOrder, secondOrder));

        List<OrderResponse> responses = orderService.getAllOrders();

        assertEquals(2, responses.size());
        assertEquals(72L, responses.get(0).getId());
        assertEquals("Buyer Seven", responses.get(0).getBuyerName());
        assertEquals(OrderStatus.PENDING, responses.get(0).getStatus());
        assertEquals(new BigDecimal("34.99"), responses.get(0).getItems().get(0).getLineTotal());
        assertEquals(73L, responses.get(1).getId());
        assertEquals("Buyer Eight", responses.get(1).getBuyerName());
        assertEquals(OrderStatus.CANCELLED, responses.get(1).getStatus());
        assertEquals(2, responses.get(1).getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("39.98"), responses.get(1).getItems().get(0).getLineTotal());
    }

    @Test
    void getOrdersForSellerReturnsOnlyOrdersContainingSellerProducts() {
        User buyer = buildUser(1L, "Buyer One");
        User targetSeller = buildUser(10L, "Target Seller");
        User otherSeller = buildUser(11L, "Other Seller");
        Product sellerProduct = buildProduct(40L, "Monitor", new BigDecimal("199.99"), 2, targetSeller);
        Product otherProduct = buildProduct(41L, "Chair", new BigDecimal("89.99"), 5, otherSeller);
        CustomerOrder matchingOrder = buildOrder(
                90L,
                buyer,
                LocalDateTime.of(2026, 4, 1, 9, 0),
                OrderStatus.PENDING,
                new BigDecimal("199.99"),
                buildOrderItem(sellerProduct, 1, new BigDecimal("199.99"))
        );
        CustomerOrder nonMatchingOrder = buildOrder(
                91L,
                buyer,
                LocalDateTime.of(2026, 4, 1, 11, 0),
                OrderStatus.PENDING,
                new BigDecimal("89.99"),
                buildOrderItem(otherProduct, 1, new BigDecimal("89.99"))
        );
        when(customerOrderRepository.findDistinctByItemsProductSellerIdOrderByOrderDateDesc(10L))
                .thenReturn(List.of(matchingOrder));

        List<OrderResponse> responses = orderService.getOrdersForSeller(10L);

        assertEquals(1, responses.size());
        assertEquals(90L, responses.get(0).getId());
        assertEquals("Monitor", responses.get(0).getItems().get(0).getProductName());
    }

    @Test
    void getOrdersForSellerIgnoresItemsWithoutProductOrSeller() {
        User buyer = buildUser(2L, "Buyer Two");
        User targetSeller = buildUser(15L, "Target Seller");
        Product matchingProduct = buildProduct(42L, "Webcam", new BigDecimal("49.99"), 6, targetSeller);

        OrderItem itemWithoutProduct = buildOrderItem(null, 1, new BigDecimal("10.00"));
        Product productWithoutSeller = buildProduct(43L, "Unnamed Seller Product", new BigDecimal("15.00"), 4, null);
        OrderItem itemWithoutSeller = buildOrderItem(productWithoutSeller, 1, new BigDecimal("15.00"));

        CustomerOrder orderWithoutProduct = buildOrder(
                92L,
                buyer,
                LocalDateTime.of(2026, 4, 2, 12, 0),
                OrderStatus.PENDING,
                new BigDecimal("10.00"),
                itemWithoutProduct
        );
        CustomerOrder orderWithoutSeller = buildOrder(
                93L,
                buyer,
                LocalDateTime.of(2026, 4, 2, 12, 30),
                OrderStatus.PENDING,
                new BigDecimal("15.00"),
                itemWithoutSeller
        );
        CustomerOrder matchingOrder = buildOrder(
                94L,
                buyer,
                LocalDateTime.of(2026, 4, 2, 13, 0),
                OrderStatus.CONFIRMED,
                new BigDecimal("49.99"),
                buildOrderItem(matchingProduct, 1, new BigDecimal("49.99"))
        );
        when(customerOrderRepository.findDistinctByItemsProductSellerIdOrderByOrderDateDesc(15L))
                .thenReturn(List.of(matchingOrder));

        List<OrderResponse> responses = orderService.getOrdersForSeller(15L);

        assertEquals(1, responses.size());
        assertEquals(94L, responses.get(0).getId());
        assertEquals("Webcam", responses.get(0).getItems().get(0).getProductName());
    }

    private OrderItemRequest buildOrderItemRequest(Long productId, Integer quantity) {
        return OrderItemRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
    }

    private User buildUser(Long id, String fullName) {
        return User.builder()
                .id(id)
                .fullName(fullName)
                .email(fullName.toLowerCase().replace(" ", ".") + "@example.com")
                .password("secret")
                .enabled(true)
                .build();
    }

    private Product buildProduct(Long id, String name, BigDecimal price, Integer stock, User seller) {
        return Product.builder()
                .id(id)
                .name(name)
                .description(name + " description")
                .price(price)
                .stock(stock)
                .imageUrl("https://cdn.example.com/" + id + ".png")
                .seller(seller)
                .build();
    }

    private OrderItem buildOrderItem(Product product, Integer quantity, BigDecimal priceAtPurchase) {
        return OrderItem.builder()
                .product(product)
                .quantity(quantity)
                .priceAtPurchase(priceAtPurchase)
                .build();
    }

    private CustomerOrder buildOrder(
            Long id,
            User buyer,
            LocalDateTime orderDate,
            OrderStatus status,
            BigDecimal totalAmount,
            OrderItem... items
    ) {
        CustomerOrder order = CustomerOrder.builder()
                .id(id)
                .buyer(buyer)
                .orderDate(orderDate)
                .status(status)
                .totalAmount(totalAmount)
                .build();

        for (OrderItem item : items) {
            order.addOrderItem(item);
        }

        return order;
    }
}
