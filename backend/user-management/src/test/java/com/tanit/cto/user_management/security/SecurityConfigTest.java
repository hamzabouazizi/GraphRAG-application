package com.tanit.cto.user_management.security;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    // Test filterChain() builds a SecurityFilterChain without exceptions
    @Test
    void filterChain_buildsWithoutErrors() throws Exception {
        SecurityConfig config = new SecurityConfig(Mockito.mock(JwtFilter.class));
        HttpSecurity http = Mockito.mock(HttpSecurity.class, Mockito.RETURNS_DEEP_STUBS);
        assertDoesNotThrow(() -> config.filterChain(http, Mockito.mock(JwtUtil.class)));
    }

    // Test corsConfigurationSource() returns correct allowed origins/methods
    @Test
    void corsConfigurationSource_hasExpectedSettings() {
        SecurityConfig config = new SecurityConfig(Mockito.mock(JwtFilter.class));

        CorsConfigurationSource source = config.corsConfigurationSource();

        // Cast to UrlBasedCorsConfigurationSource to access internal map
        UrlBasedCorsConfigurationSource urlSource = (UrlBasedCorsConfigurationSource) source;

        var cors = urlSource.getCorsConfigurations().get("/**");

        assertNotNull(cors);
        assertTrue(cors.getAllowedOrigins().contains("http://localhost"));
        assertTrue(cors.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(cors.getAllowedMethods().contains("GET"));
        assertTrue(cors.getAllowedMethods().contains("POST"));
        assertTrue(cors.getAllowedMethods().contains("PUT"));
        assertTrue(cors.getAllowedMethods().contains("DELETE"));
        assertTrue(cors.getAllowedMethods().contains("OPTIONS"));
        assertTrue(cors.getAllowedHeaders().contains("*"));
        assertTrue(cors.getAllowCredentials());
    }

}
