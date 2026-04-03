package com.hasan.marketplace.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.hasan.marketplace.MarketplaceApplication;
import com.hasan.marketplace.entity.Category;
import com.hasan.marketplace.entity.CustomerOrder;
import com.hasan.marketplace.entity.OrderItem;
import com.hasan.marketplace.entity.OrderStatus;
import com.hasan.marketplace.entity.Product;
import com.hasan.marketplace.entity.Role;
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.repository.CategoryRepository;
import com.hasan.marketplace.repository.CustomerOrderRepository;
import com.hasan.marketplace.repository.ProductRepository;
import com.hasan.marketplace.repository.RoleRepository;
import com.hasan.marketplace.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = MarketplaceApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:marketplace_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password="
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderIntegrationTest {

    private static final String SELLER_EMAIL = "order-seller@test.com";
    private static final String BUYER_EMAIL = "order-buyer@test.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Product savedProduct;
    private CustomerOrder savedOrder;

    @BeforeEach
    void setUp() {
        customerOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.findByEmail(SELLER_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(BUYER_EMAIL).ifPresent(userRepository::delete);

        Role sellerRole = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        Role buyerRole = roleRepository.findByName(RoleName.BUYER).orElseThrow();
        Category phones = categoryRepository.findByNameIgnoreCase("Phones").orElseThrow();

        User seller = User.builder()
                .fullName("Order Seller")
                .email(SELLER_EMAIL)
                .password("encoded-password")
                .enabled(true)
                .roles(new HashSet<>())
                .build();
        seller.getRoles().add(sellerRole);
        seller = userRepository.save(seller);

        User buyer = User.builder()
                .fullName("Order Buyer")
                .email(BUYER_EMAIL)
                .password(passwordEncoder.encode("buyer123"))
                .enabled(true)
                .roles(new HashSet<>())
                .build();
        buyer.getRoles().add(buyerRole);
        buyer = userRepository.save(buyer);

        savedProduct = Product.builder()
                .name("Order Phone")
                .description("Product used for order integration tests")
                .price(new BigDecimal("300.00"))
                .stock(6)
                .category(phones)
                .seller(seller)
                .build();
        savedProduct = productRepository.save(savedProduct);

        savedOrder = CustomerOrder.builder()
                .buyer(buyer)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("300.00"))
                .build();
        savedOrder.addOrderItem(OrderItem.builder()
                .product(savedProduct)
                .quantity(1)
                .priceAtPurchase(new BigDecimal("300.00"))
                .build());
        savedOrder = customerOrderRepository.save(savedOrder);
    }

    @Test
    @WithMockUser(username = BUYER_EMAIL, roles = "BUYER")
    void checkoutPage_shouldShowSelectedProduct() throws Exception {
        mockMvc.perform(get("/checkout/" + savedProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(content().string(containsString("Order Phone")));
    }

    @Test
    @WithMockUser(username = BUYER_EMAIL, roles = "BUYER")
    void orderDetails_shouldShowBuyerOrder() throws Exception {
        mockMvc.perform(get("/orders/" + savedOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("order-details"))
                .andExpect(content().string(containsString("Order Details")));
    }

    @Test
    @WithMockUser(username = BUYER_EMAIL, roles = "BUYER")
    void placeOrder_shouldShowErrorWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(post("/orders")
                        .with(csrf())
                        .param("productId", "99999")
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("error-page"))
                .andExpect(content().string(containsString("Product not found")));
    }

    @Test
    @WithMockUser(username = SELLER_EMAIL, roles = "SELLER")
    void orders_shouldRejectSellerRole() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isForbidden());
    }
}
