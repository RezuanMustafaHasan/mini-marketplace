package com.hasan.marketplace.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.hasan.marketplace.entity.Role;
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.repository.UserRepository;
import java.util.Optional;
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
    void loadUserByUsername_shouldReturnUserDetails() {
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(User.builder()
                .email("seller@example.com")
                .password("encoded")
                .enabled(true)
                .roles(Set.of(Role.builder().name(RoleName.SELLER).build()))
                .build()));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("seller@example.com");

        assertEquals("seller@example.com", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_SELLER")));
    }

    @Test
    void loadUserByUsername_shouldThrowWhenUserIsMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("missing@example.com")
        );
    }
}
