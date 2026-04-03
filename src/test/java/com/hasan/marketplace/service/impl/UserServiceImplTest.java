package com.hasan.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hasan.marketplace.dto.UserRegistrationRequest;
import com.hasan.marketplace.entity.Role;
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.exception.ResourceNotFoundException;
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
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registerUserSavesEnabledUserWithEncodedPasswordAndRole() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .fullName("Buyer One")
                .email("buyer@example.com")
                .password("secret123")
                .role(RoleName.BUYER)
                .build();
        Role buyerRole = Role.builder().id(2L).name(RoleName.BUYER).build();

        when(userRepository.existsByEmail("buyer@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.BUYER)).thenReturn(Optional.of(buyerRole));
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.registerUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User persistedUser = userCaptor.getValue();
        assertEquals("Buyer One", persistedUser.getFullName());
        assertEquals("buyer@example.com", persistedUser.getEmail());
        assertEquals("encoded-secret", persistedUser.getPassword());
        assertTrue(persistedUser.isEnabled());
        assertNotNull(persistedUser.getRoles());
        assertTrue(persistedUser.getRoles().contains(buyerRole));
        assertSame(persistedUser, savedUser);
    }

    @Test
    void registerUserRejectsDuplicateEmail() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("taken@example.com")
                .build();
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(request)
        );

        assertEquals("Email is already registered", exception.getMessage());
        verify(roleRepository, never()).findByName(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUserThrowsWhenRequestedRoleDoesNotExist() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .fullName("Seller One")
                .email("seller@example.com")
                .password("secret123")
                .role(RoleName.SELLER)
                .build();

        when(userRepository.existsByEmail("seller@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.SELLER)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.registerUser(request)
        );

        assertEquals("Role not found: SELLER", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findByEmailReturnsStoredUser() {
        User user = User.builder()
                .id(5L)
                .fullName("Existing User")
                .email("existing@example.com")
                .build();
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(user));

        User result = userService.findByEmail("existing@example.com");

        assertSame(user, result);
    }

    @Test
    void findByEmailThrowsWhenUserIsMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.findByEmail("missing@example.com")
        );

        assertEquals("User not found with email: missing@example.com", exception.getMessage());
    }
}
