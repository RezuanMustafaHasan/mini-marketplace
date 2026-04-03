package com.hasan.marketplace.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.hasan.marketplace.entity.Role;
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.repository.UserRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsernameMapsCredentialsAuthoritiesAndEnabledFlag() {
        User user = User.builder()
                .email("seller@example.com")
                .password("encoded-password")
                .enabled(false)
                .roles(Set.of(
                        Role.builder().name(RoleName.SELLER).build(),
                        Role.builder().name(RoleName.ADMIN).build()
                ))
                .build();
        when(userRepository.findByEmail("seller@example.com")).thenReturn(java.util.Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("seller@example.com");

        assertEquals("seller@example.com", userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
        assertFalse(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_SELLER")));
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsernameThrowsWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(java.util.Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("missing@example.com")
        );

        assertEquals("User not found with email: missing@example.com", exception.getMessage());
    }
}
