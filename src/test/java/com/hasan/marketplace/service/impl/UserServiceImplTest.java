package com.hasan.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hasan.marketplace.dto.UserRegistrationRequest;
import com.hasan.marketplace.entity.Role;
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.repository.RoleRepository;
import com.hasan.marketplace.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    void registerUser_shouldSaveUser() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .fullName("Buyer One")
                .email("buyer@example.com")
                .password("secret123")
                .role(RoleName.BUYER)
                .build();

        when(userRepository.existsByEmail("buyer@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.BUYER)).thenReturn(Optional.of(Role.builder().name(RoleName.BUYER).build()));
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.registerUser(request);

        assertEquals("buyer@example.com", savedUser.getEmail());
        assertEquals("encoded", savedUser.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(UserRegistrationRequest.builder()
                        .email("taken@example.com")
                        .build())
        );

        assertEquals("Email is already registered", exception.getMessage());
        verify(userRepository, never()).save(any());
    }
}
