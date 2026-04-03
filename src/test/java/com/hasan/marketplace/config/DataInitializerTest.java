package com.hasan.marketplace.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hasan.marketplace.entity.Role;
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.repository.RoleRepository;
import com.hasan.marketplace.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void runCreatesMissingRolesAndDefaultAdminUser() {
        Role adminRole = Role.builder().id(1L).name(RoleName.ADMIN).build();

        when(roleRepository.existsByName(RoleName.ADMIN)).thenReturn(false);
        when(roleRepository.existsByName(RoleName.SELLER)).thenReturn(false);
        when(roleRepository.existsByName(RoleName.BUYER)).thenReturn(false);
        when(userRepository.existsByEmail("admin@marketplace.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("admin123")).thenReturn("encoded-admin123");

        dataInitializer.run();

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, org.mockito.Mockito.times(3)).save(roleCaptor.capture());
        assertEquals(
                java.util.List.of(RoleName.ADMIN, RoleName.SELLER, RoleName.BUYER),
                roleCaptor.getAllValues().stream().map(Role::getName).toList()
        );

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User admin = userCaptor.getValue();
        assertEquals("Marketplace Admin", admin.getFullName());
        assertEquals("admin@marketplace.com", admin.getEmail());
        assertEquals("encoded-admin123", admin.getPassword());
        assertTrue(admin.isEnabled());
        assertTrue(admin.getRoles().contains(adminRole));
    }

    @Test
    void runSkipsSavingExistingRolesAndAdmin() {
        when(roleRepository.existsByName(RoleName.ADMIN)).thenReturn(true);
        when(roleRepository.existsByName(RoleName.SELLER)).thenReturn(true);
        when(roleRepository.existsByName(RoleName.BUYER)).thenReturn(true);
        when(userRepository.existsByEmail("admin@marketplace.com")).thenReturn(true);

        dataInitializer.run();

        verify(roleRepository, never()).save(any(Role.class));
        verify(roleRepository, never()).findByName(RoleName.ADMIN);
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }
}
