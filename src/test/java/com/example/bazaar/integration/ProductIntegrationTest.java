package com.example.bazaar.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.hasan.marketplace.MarketplaceApplication;
import com.hasan.marketplace.entity.Category;
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
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class ProductIntegrationTest {

    private static final String SELLER_EMAIL = "product-seller@test.com";

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

    private Long categoryId;

    @BeforeEach
    void setUp() {
        customerOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.findByEmail(SELLER_EMAIL).ifPresent(userRepository::delete);

        Role sellerRole = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        Category phones = categoryRepository.findByNameIgnoreCase("Phones").orElseThrow();
        categoryId = phones.getId();

        User seller = User.builder()
                .fullName("Product Seller")
                .email(SELLER_EMAIL)
                .password("encoded-password")
                .enabled(true)
                .roles(new HashSet<>())
                .build();
        seller.getRoles().add(sellerRole);
        seller = userRepository.save(seller);

        Product product = Product.builder()
                .name("Demo Phone")
                .description("Simple test product")
                .price(new BigDecimal("499.00"))
                .stock(5)
                .category(phones)
                .seller(seller)
                .build();
        productRepository.save(product);
    }

    @Test
    @WithMockUser(username = SELLER_EMAIL, roles = "SELLER")
    void sellerProducts_shouldShowCurrentSellerProducts() throws Exception {
        mockMvc.perform(get("/seller/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("seller-products"))
                .andExpect(content().string(containsString("My Products")))
                .andExpect(content().string(containsString("Demo Phone")));
    }

    @Test
    @WithMockUser(username = SELLER_EMAIL, roles = "SELLER")
    void createProduct_shouldShowValidationErrorForBlankName() throws Exception {
        mockMvc.perform(post("/seller/products")
                        .with(csrf())
                        .param("name", "")
                        .param("description", "Missing name")
                        .param("price", "250.00")
                        .param("stock", "3")
                        .param("categoryId", String.valueOf(categoryId)))
                .andExpect(status().isOk())
                .andExpect(view().name("product-form"))
                .andExpect(model().attributeHasFieldErrors("product", "name"));
    }

    @Test
    @WithMockUser(username = "buyer@test.com", roles = "BUYER")
    void sellerProducts_shouldRejectBuyerRole() throws Exception {
        mockMvc.perform(get("/seller/products"))
                .andExpect(status().isForbidden());
    }
}
