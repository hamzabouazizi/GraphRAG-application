package com.tanit.cto.user_management.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitingFilter filter;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws IOException {
        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        filter = new RateLimitingFilter(jwtUtil, true);
    }

    @AfterEach
    void tearDown() {
        filter.resetCounters();
    }

    @Test
    void whenUnderGlobalLimit_thenRequestPasses() throws Exception {
        when(request.getRequestURI()).thenReturn("/any");
        when(request.getRemoteAddr()).thenReturn("1.1.1.1");

        for (int i = 0; i < 100; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(100)).doFilter(any(), any());
        verify(response, never()).setStatus(429);
    }

    @Test
    void whenOverGlobalLimit_thenBlocked() throws Exception {
        when(request.getRequestURI()).thenReturn("/any");
        when(request.getRemoteAddr()).thenReturn("2.2.2.2");

        for (int i = 0; i < 101; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(response, atLeastOnce()).setStatus(429);
        assertTrue(responseWriter.toString().contains("Too many requests from IP"));

    }

    @Test
    void whenSignupLimitExceeded_thenBlocked() throws Exception {
        when(request.getRequestURI()).thenReturn("/signup");
        when(request.getRemoteAddr()).thenReturn("3.3.3.3");
        when(request.getContentType()).thenReturn("application/json");
        when(request.getInputStream())
                .thenReturn(new MockServletInputStream("{\"email\":\"test@example.com\"}".getBytes()));

        for (int i = 0; i < 6; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(response, atLeastOnce()).setStatus(429);
        assertTrue(responseWriter.toString().contains("Too many signups from this IP"));
    }

    @Test
    void whenLoginUserLimitExceeded_thenBlocked() throws Exception {
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getRemoteAddr()).thenReturn("4.4.4.4");
        when(request.getContentType()).thenReturn(null);
        when(request.getParameter("email")).thenReturn("user@example.com");

        for (int i = 0; i < 6; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(response, atLeastOnce()).setStatus(429);
        assertTrue(responseWriter.toString().contains("Too many login attempts for this user"));
    }

    @Test
    void whenLoginIpLimitExceeded_thenBlocked() throws Exception {
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getRemoteAddr()).thenReturn("5.5.5.5");
        when(request.getContentType()).thenReturn("application/json");
        when(request.getInputStream())
                .thenReturn(new MockServletInputStream("{\"email\":\"iptest@example.com\"}".getBytes()));

        for (int i = 0; i < 21; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(response, atLeastOnce()).setStatus(429);
        assertTrue(responseWriter.toString().contains("Too many login attempts from this IP"));
    }

    @Test
    void whenJwtExtractsEmail_thenRespectsLoginUserLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getRemoteAddr()).thenReturn("6.6.6.6");
        when(request.getHeader("Authorization")).thenReturn("Bearer fake-jwt");

        when(jwtUtil.extractEmail("fake-jwt")).thenReturn("jwtuser@example.com");

        for (int i = 0; i < 6; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(jwtUtil, atLeastOnce()).extractEmail("fake-jwt");
        verify(response, atLeastOnce()).setStatus(429);
        assertTrue(responseWriter.toString().contains("Too many login attempts for this user"));

    }

    @Test
    void cachedBodyRequest_replaysCorrectly() throws Exception {
        String json = "{\"email\":\"cached@example.com\"}";
        lenient().when(request.getRequestURI()).thenReturn("/signup");
        lenient().when(request.getRemoteAddr()).thenReturn("7.7.7.7");
        lenient().when(request.getContentType()).thenReturn("application/json");
        lenient().when(request.getCharacterEncoding()).thenReturn("UTF-8");
        lenient().when(request.getInputStream()).thenReturn(new MockServletInputStream(json.getBytes()));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        verify(response, never()).setStatus(429);
    }

    @Test
    void whenRateLimitDisabled_thenAllRequestsPass() throws Exception {
        RateLimitingFilter disabledFilter = new RateLimitingFilter(jwtUtil, false);

        for (int i = 0; i < 200; i++) {
            disabledFilter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(200)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        verify(response, never()).setStatus(429);
        assertEquals("", responseWriter.toString());
    }

    /**
     * Helper class to simulate ServletInputStream for mock requests.
     */
    static class MockServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream bais;

        MockServletInputStream(byte[] bytes) {
            this.bais = new ByteArrayInputStream(bytes);
        }

        @Override
        public int read() {
            return bais.read();
        }

        @Override
        public boolean isFinished() {
            return bais.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
        }
    }
}
