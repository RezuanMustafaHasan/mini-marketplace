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
import com.hasan.marketplace.entity.Product;
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.repository.CategoryRepository;
import com.hasan.marketplace.repository.CustomerOrderRepository;
import com.hasan.marketplace.repository.ProductRepository;
import com.hasan.marketplace.repository.UserRepository;
import java.math.BigDecimal;
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
class ProductIntegrationTest {

    private static final String SELLER_EMAIL = "product-seller@test.com";
    private static final String SELLER_PASSWORD = "seller123";

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
    }

    @Test
    void completeProductFlow_shouldCreateAndUpdateProductThroughHttpRequests() throws Exception {
        Category phones = categoryRepository.findByNameIgnoreCase("Phones").orElseThrow();

        // 1. Register a seller.
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Product Seller")
                        .param("email", SELLER_EMAIL)
                        .param("password", SELLER_PASSWORD)
                        .param("role", RoleName.SELLER.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        User savedSeller = userRepository.findByEmail(SELLER_EMAIL).orElseThrow();
        assertThat(savedSeller.getRoles())
                .extracting(role -> role.getName())
                .contains(RoleName.SELLER);

        // 2. Login as the seller.
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", SELLER_EMAIL)
                        .param("password", SELLER_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"))
                .andExpect(authenticated().withUsername(SELLER_EMAIL))
                .andReturn();

        MockHttpSession sellerSession = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(sellerSession).isNotNull();

        // 3. Open the create form and create a product.
        mockMvc.perform(get("/seller/products/new").session(sellerSession))
                .andExpect(status().isOk())
                .andExpect(view().name("product-form"))
                .andExpect(content().string(containsString("Add Product")));

        mockMvc.perform(post("/seller/products")
                        .session(sellerSession)
                        .with(csrf())
                        .param("name", "Demo Phone")
                        .param("description", "Simple test product")
                        .param("price", "499.00")
                        .param("stock", "5")
                        .param("categoryId", String.valueOf(phones.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seller/products"));

        Product savedProduct = productRepository.findAllByOrderByIdDesc().get(0);
        assertThat(savedProduct.getName()).isEqualTo("Demo Phone");
        assertThat(savedProduct.getDescription()).isEqualTo("Simple test product");
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("499.00"));
        assertThat(savedProduct.getStock()).isEqualTo(5);
        assertThat(savedProduct.getCategory().getName()).isEqualTo("Phones");
        assertThat(savedProduct.getSeller().getEmail()).isEqualTo(SELLER_EMAIL);

        // 4. Verify the product appears on seller and public pages.
        mockMvc.perform(get("/seller/products").session(sellerSession))
                .andExpect(status().isOk())
                .andExpect(view().name("seller-products"))
                .andExpect(content().string(containsString("Demo Phone")));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(content().string(containsString("Demo Phone")));

        mockMvc.perform(get("/products/" + savedProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("product-details"))
                .andExpect(content().string(containsString("Demo Phone")));

        // 5. Update the product and verify the new data.
        mockMvc.perform(get("/seller/products/edit/" + savedProduct.getId()).session(sellerSession))
                .andExpect(status().isOk())
                .andExpect(view().name("product-form"))
                .andExpect(content().string(containsString("Edit Product")));

        mockMvc.perform(post("/seller/products/update/" + savedProduct.getId())
                        .session(sellerSession)
                        .with(csrf())
                        .param("name", "Updated Demo Phone")
                        .param("description", "Updated product description")
                        .param("price", "549.00")
                        .param("stock", "7")
                        .param("categoryId", String.valueOf(phones.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seller/products"));

        Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getName()).isEqualTo("Updated Demo Phone");
        assertThat(updatedProduct.getDescription()).isEqualTo("Updated product description");
        assertThat(updatedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("549.00"));
        assertThat(updatedProduct.getStock()).isEqualTo(7);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(content().string(containsString("Updated Demo Phone")));

        mockMvc.perform(get("/products/" + updatedProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("product-details"))
                .andExpect(content().string(containsString("Updated Demo Phone")))
                .andExpect(content().string(containsString("Updated product description")));
    }
}
