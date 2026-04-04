package com.hasan.marketplace.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
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
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.repository.CategoryRepository;
import com.hasan.marketplace.repository.CustomerOrderRepository;
import com.hasan.marketplace.repository.ProductRepository;
import com.hasan.marketplace.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
    private static final String SELLER_PASSWORD = "seller123";
    private static final String BUYER_EMAIL = "order-buyer@test.com";
    private static final String BUYER_PASSWORD = "buyer123";

    @Autowired
    private MockMvc mockMvc;

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
        userRepository.findByEmail(SELLER_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(BUYER_EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    void completeAuthProductAndOrderFlow_shouldWorkFromHttpRequestToDatabase() throws Exception {
        Category phones = categoryRepository.findByNameIgnoreCase("Phones").orElseThrow();

        // 1. Register a seller.
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Order Seller")
                        .param("email", SELLER_EMAIL)
                        .param("password", SELLER_PASSWORD)
                        .param("role", RoleName.SELLER.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        User savedSeller = userRepository.findByEmail(SELLER_EMAIL).orElseThrow();
        assertThat(savedSeller.getRoles())
                .extracting(role -> role.getName())
                .contains(RoleName.SELLER);

        // 2. Register a buyer.
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Order Buyer")
                        .param("email", BUYER_EMAIL)
                        .param("password", BUYER_PASSWORD)
                        .param("role", RoleName.BUYER.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        User savedBuyer = userRepository.findByEmail(BUYER_EMAIL).orElseThrow();
        assertThat(savedBuyer.getRoles())
                .extracting(role -> role.getName())
                .contains(RoleName.BUYER);

        // 3. Login as seller and create a product.
        MvcResult sellerLoginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", SELLER_EMAIL)
                        .param("password", SELLER_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"))
                .andExpect(authenticated().withUsername(SELLER_EMAIL))
                .andReturn();

        MockHttpSession sellerSession = (MockHttpSession) sellerLoginResult.getRequest().getSession(false);
        assertThat(sellerSession).isNotNull();

        mockMvc.perform(post("/seller/products")
                        .session(sellerSession)
                        .with(csrf())
                        .param("name", "Order Phone")
                        .param("description", "Product used for the full integration test")
                        .param("price", "300.00")
                        .param("stock", "6")
                        .param("categoryId", String.valueOf(phones.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seller/products"));

        Product savedProduct = productRepository.findAllByOrderByIdDesc().get(0);
        assertThat(savedProduct.getName()).isEqualTo("Order Phone");
        assertThat(savedProduct.getSeller().getEmail()).isEqualTo(SELLER_EMAIL);
        assertThat(savedProduct.getCategory().getName()).isEqualTo("Phones");
        assertThat(savedProduct.getStock()).isEqualTo(6);

        // 4. Login as buyer and go through the order flow.
        MvcResult buyerLoginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", BUYER_EMAIL)
                        .param("password", BUYER_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"))
                .andExpect(authenticated().withUsername(BUYER_EMAIL))
                .andReturn();

        MockHttpSession buyerSession = (MockHttpSession) buyerLoginResult.getRequest().getSession(false);
        assertThat(buyerSession).isNotNull();

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(content().string(containsString("Order Phone")));

        mockMvc.perform(get("/checkout/" + savedProduct.getId()).session(buyerSession))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(content().string(containsString("Order Phone")));

        mockMvc.perform(post("/orders")
                        .session(buyerSession)
                        .with(csrf())
                        .param("productId", String.valueOf(savedProduct.getId()))
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        List<CustomerOrder> orders = customerOrderRepository.findAll();
        assertThat(orders).hasSize(1);

        CustomerOrder savedOrder = orders.get(0);
        assertThat(savedOrder.getBuyer().getEmail()).isEqualTo(BUYER_EMAIL);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(savedOrder.getItems()).hasSize(1);

        OrderItem savedOrderItem = savedOrder.getItems().get(0);
        assertThat(savedOrderItem.getProduct().getId()).isEqualTo(savedProduct.getId());
        assertThat(savedOrderItem.getQuantity()).isEqualTo(2);
        assertThat(savedOrderItem.getPriceAtPurchase()).isEqualByComparingTo(new BigDecimal("300.00"));

        Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(4);

        mockMvc.perform(get("/orders").session(buyerSession))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(content().string(containsString("My Orders")))
                .andExpect(content().string(containsString("600.00")));

        mockMvc.perform(get("/orders/" + savedOrder.getId()).session(buyerSession))
                .andExpect(status().isOk())
                .andExpect(view().name("order-details"))
                .andExpect(content().string(containsString("Order Details")))
                .andExpect(content().string(containsString("Order Phone")));
    }
}
