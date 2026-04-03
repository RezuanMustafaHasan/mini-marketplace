package com.hasan.marketplace.config;

import com.hasan.marketplace.entity.Role;
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.repository.RoleRepository;
import com.hasan.marketplace.repository.UserRepository;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        ensureRoleExists(RoleName.ADMIN);
        ensureRoleExists(RoleName.SELLER);
        ensureRoleExists(RoleName.BUYER);

        createDefaultAdminUser();
    }

    private void ensureRoleExists(RoleName roleName) {
        if (!roleRepository.existsByName(roleName)) {
            roleRepository.save(Role.builder().name(roleName).build());
        }
    }

    private void createDefaultAdminUser() {
        String adminEmail = "admin@marketplace.com";

        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role must exist before creating admin user"));

        User admin = User.builder()
                .fullName("Marketplace Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode("admin123"))
                .enabled(true)
                .roles(new HashSet<>())
                .build();

        admin.getRoles().add(adminRole);
        userRepository.save(admin);
    }
}

