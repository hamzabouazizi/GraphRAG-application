package com.tanit.cto.user_management.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    // No Authorization header
    // Expect: filter calls chain.doFilter without setting authentication
    @Test
    void doFilterInternal_NoAuthorizationHeader_DoesNothing() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    // Authorization header without Bearer
    // expectation: filter calls chain.doFilter without setting authentication so
    // authentication remains null
    @Test
    void doFilterInternal_HeaderWithoutBearer_DoesNothing() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    // Valid Bearer token
    // Mock jwtUtil.extractEmail(token) ? return email
    // Mock userDetailsService.loadUserByUsername(email) ? return UserDetails
    // Mock jwtUtil.validateToken(token) ? return true
    // Expect: SecurityContextHolder.getContext().getAuthentication() is set
    @Test
    void doFilterInternal_ValidToken_SetsAuthentication() throws ServletException, IOException {
        String token = "validToken";
        String email = "example@example.com";
        UserDetails userDetails = new User(email, "password", Collections.emptyList());

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtUtil.validateToken(token)).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(email, authentication.getName());

        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtUtil, times(1)).extractEmail(token);
        verify(jwtUtil, times(1)).validateToken(token);
        verify(userDetailsService, times(1)).loadUserByUsername(email);
    }

    // Invalid token
    // jwtUtil.validateToken(token) returns false
    // Expect: authentication is not set
    @Test
    void doFilterInternal_InvalidToken_DoesNotSetAuthentication() throws ServletException, IOException {
        String token = "invalidToken";
        String email = "example@example.com";
        UserDetails userDetails = new User(email, "password", Collections.emptyList());

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtUtil.validateToken(token)).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    // Authentication already set
    // Call filter again
    // Expect: it doesn’t overwrite the existing authentication
    @Test
    void doFilterInternal_AuthenticationAlreadySet_DoesNotOverwrite() throws ServletException, IOException {
        String token = "validToken";
        // stuffing a fake authentication object into storage
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alreadySet", null, Collections.emptyList()));

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        jwtFilter.doFilterInternal(request, response, filterChain);

        // Authentication should remain unchanged
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("alreadySet", authentication.getPrincipal());

        verify(filterChain, times(1)).doFilter(request, response);
    }
}
