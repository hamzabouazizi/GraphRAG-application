package com.tanit.cto.user_management.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();

        // Manually inject the secret using reflection
        Field secretField = JwtUtil.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtUtil, "testsecret123456789");
    }

    @Test
    void testGenerateAndExtractEmail() {
        String email = "example@example.com";
        String token = jwtUtil.generateToken(email);

        assertNotNull(token);

        String extractedEmail = jwtUtil.extractEmail(token);
        assertEquals(email, extractedEmail);
    }

    @Test
    void testValidateToken_Valid() {
        String email = "example@example.com";
        String token = jwtUtil.generateToken(email);

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_Expired() throws Exception {
        String email = "example@example.com";
        // Generate token that expired 1 second ago
        String token = jwtUtil.generateToken(email, -1000L);
        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_InvalidToken() {
        String invalidToken = "this.is.not.a.valid.token";
        assertFalse(jwtUtil.validateToken(invalidToken));
    }
}
