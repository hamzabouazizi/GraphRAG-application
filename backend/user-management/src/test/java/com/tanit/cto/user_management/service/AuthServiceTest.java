package com.tanit.cto.user_management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tanit.cto.user_management.model.AuthRequest;
import com.tanit.cto.user_management.model.User;
import com.tanit.cto.user_management.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // register() encodes passwords and saves the right user
    @Test
    void testRegister_ShouldSaveUserWithEncodedPassword() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("xxx xxx");

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        User savedUser = new User();
        savedUser.setEmail("test@example.com");
        savedUser.setPassword("encodedPassword");
        savedUser.setFullName("xxx xxx");
        savedUser.addRole("USER");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = authService.register(request);
        // inspect what was passed to a mocked method
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals("test@example.com", capturedUser.getEmail());
        assertEquals("encodedPassword", capturedUser.getPassword());
        assertEquals("xxx xxx", capturedUser.getFullName());
        assertTrue(capturedUser.getRoles().contains("USER"));

        assertEquals("test@example.com", result.getEmail());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals("xxx xxx", result.getFullName());
        assertTrue(result.getRoles().contains("USER"));
    }

    // login works when password matches
    @Test
    void testLogin_ShouldReturnUser_WhenPasswordMatches() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        Optional<User> result = authService.login(request);

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    // login fails when password is wrong
    @Test
    void testLogin_ShouldReturnEmpty_WhenPasswordDoesNotMatch() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        Optional<User> result = authService.login(request);

        assertTrue(result.isEmpty());
    }

    // login fails when user does not exist
    @Test
    void testLogin_ShouldReturnEmpty_WhenUserNotFound() {
        AuthRequest request = new AuthRequest();
        request.setEmail("notfound@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        Optional<User> result = authService.login(request);

        assertTrue(result.isEmpty());
    }
}
