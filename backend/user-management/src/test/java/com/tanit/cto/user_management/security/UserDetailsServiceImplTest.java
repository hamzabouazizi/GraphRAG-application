package com.tanit.cto.user_management.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.tanit.cto.user_management.model.User;
import com.tanit.cto.user_management.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private UserDetailsServiceImpl userDetailsService;

        @Test
        void loadUserByUsername_UserExists_ReturnsUserDetails() {
                User mockUser = new User();
                mockUser.setEmail("test@example.com");
                mockUser.setPassword("password123");

                when(userRepository.findByEmail("test@example.com"))
                                .thenReturn(Optional.of(mockUser));

                UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

                assertNotNull(userDetails);
                assertEquals("test@example.com", userDetails.getUsername());
                assertEquals("password123", userDetails.getPassword());
                assertTrue(userDetails.getAuthorities().stream()
                                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));

                verify(userRepository, times(1)).findByEmail("test@example.com");
        }

        @Test
        void loadUserByUsername_UserDoesNotExist_ThrowsException() {
                // Arrange: no user found
                when(userRepository.findByEmail("unknown@example.com"))
                                .thenReturn(Optional.empty());

                // Act + Assert
                assertThrows(UsernameNotFoundException.class,
                                () -> userDetailsService.loadUserByUsername("unknown@example.com"));

                // Verify repository call
                verify(userRepository, times(1)).findByEmail("unknown@example.com");
        }
}
