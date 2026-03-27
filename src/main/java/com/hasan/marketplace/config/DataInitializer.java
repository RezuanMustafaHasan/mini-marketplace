package com.hasan.marketplace.config;

import com.hasan.marketplace.entity.Category;
import com.hasan.marketplace.entity.Role;
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.repository.CategoryRepository;
import com.hasan.marketplace.repository.ProductRepository;
import com.hasan.marketplace.repository.RoleRepository;
import com.hasan.marketplace.repository.UserRepository;
import java.util.List;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "Phones",
            "Tablet",
            "Laptop",
            "Smart Watch",
            "Gadgets",
            "Accessories"
    );

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        ensureRoleExists(RoleName.ADMIN);
        ensureRoleExists(RoleName.SELLER);
        ensureRoleExists(RoleName.BUYER);

        ensureDefaultCategories();
        assignFallbackCategoryToExistingProducts();
        createDefaultAdminUser();
    }

    private void ensureRoleExists(RoleName roleName) {
        if (!roleRepository.existsByName(roleName)) {
            roleRepository.save(Role.builder().name(roleName).build());
        }
    }

    private void ensureDefaultCategories() {
        for (String categoryName : DEFAULT_CATEGORIES) {
            if (!categoryRepository.existsByNameIgnoreCase(categoryName)) {
                categoryRepository.save(Category.builder().name(categoryName).build());
            }
        }
    }

    private void assignFallbackCategoryToExistingProducts() {
        Category fallbackCategory = categoryRepository.findByNameIgnoreCase("Accessories")
                .orElseThrow(() -> new IllegalStateException("Accessories category must exist before product backfill"));

        productRepository.findByCategoryIsNull()
                .forEach(product -> product.setCategory(fallbackCategory));
    }

    private void createDefaultAdminUser() {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role must exist before creating admin user"));

        User admin = User.builder()
                .fullName("Marketplace Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .enabled(true)
                .roles(new HashSet<>())
                .build();

        admin.getRoles().add(adminRole);
        userRepository.save(admin);
    }
}
