package com.hasan.marketplace.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MarketplaceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @BeforeEach
    void setUp() {
        customerOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.findByEmail("seller@test.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("buyer@test.com").ifPresent(userRepository::delete);

        Role sellerRole = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        Role buyerRole = roleRepository.findByName(RoleName.BUYER).orElseThrow();
        Category phones = categoryRepository.findByNameIgnoreCase("Phones").orElseThrow();

        User seller = User.builder()
                .fullName("Seller Test")
                .email("seller@test.com")
                .password("encoded")
                .enabled(true)
                .roles(new HashSet<>())
                .build();
        seller.getRoles().add(sellerRole);
        seller = userRepository.save(seller);

        User buyer = User.builder()
                .fullName("Buyer Test")
                .email("buyer@test.com")
                .password("encoded")
                .enabled(true)
                .roles(new HashSet<>())
                .build();
        buyer.getRoles().add(buyerRole);
        buyer = userRepository.save(buyer);

        Product product = Product.builder()
                .name("Integration Phone")
                .description("Phone used in integration testing")
                .price(new BigDecimal("650.00"))
                .stock(8)
                .seller(seller)
                .category(phones)
                .build();
        product = productRepository.save(product);

        CustomerOrder order = CustomerOrder.builder()
                .buyer(buyer)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("650.00"))
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(1)
                .priceAtPurchase(new BigDecimal("650.00"))
                .build();
        order.addOrderItem(item);
        customerOrderRepository.save(order);
    }

    @Test
    void publicProductsPageIsAccessible() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Integration Phone")))
                .andExpect(content().string(containsString("Phones")));
    }

    @Test
    void sellerRoutesAreProtectedFromAnonymousUsers() throws Exception {
        mockMvc.perform(get("/seller/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "buyer@test.com", roles = "BUYER")
    void sellerRoutesRejectBuyerRole() throws Exception {
        mockMvc.perform(get("/seller/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@marketplace.com", roles = "ADMIN")
    void adminCategoryRoutesAllowAdmin() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Category Management")));
    }

    @Test
    @WithMockUser(username = "seller@test.com", roles = "SELLER")
    void adminCategoryRoutesRejectSeller() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "buyer@test.com", roles = "BUYER")
    void buyerOrderRoutesAllowBuyer() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("My Orders")));
    }

    @Test
    void buyerOrderRoutesRedirectAnonymousUsers() throws Exception {
        mockMvc.perform(post("/orders").with(csrf()).param("productId", "1").param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
