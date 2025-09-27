package com.tanit.cto.user_management.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class RateLimitingFilterIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private RateLimitingFilter rateLimitingFilter;

        @MockitoBean
        private JwtUtil jwtUtil;

        @BeforeEach
        void resetRateLimitCounters() {
                rateLimitingFilter.resetCounters();
                rateLimitingFilter.setEnabled(true);
        }

        @Test
        void login_shouldBeRateLimited_afterMaxRequests() throws Exception {
                String signupRequest = """
                                {
                                  "fullName": "Test User",
                                  "email": "test@example.com",
                                  "password": "secret"
                                }
                                """;
                mockMvc.perform(post("/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(signupRequest))
                                .andExpect(status().isOk());
                String requestBody = """
                                { "email": "test@example.com", "password": "secret" }
                                """;

                for (int i = 0; i < 5; i++) {
                        mockMvc.perform(post("/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                                        .andExpect(result -> {
                                                int status = result.getResponse().getStatus();
                                                if (status != HttpServletResponse.SC_OK &&
                                                                status != HttpServletResponse.SC_UNAUTHORIZED) {
                                                        throw new AssertionError("Unexpected status: " + status);
                                                }
                                        });
                }

                mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isTooManyRequests());
        }

        @Test
        void signup_shouldBeRateLimited_afterMaxRequests() throws Exception {
                String requestBody = """
                                {
                                  "fullName": "New TestUser",
                                  "email": "newtestuser@example.com",
                                  "password": "secret"
                                }
                                """;

                for (int i = 0; i < 5; i++) {
                        mockMvc.perform(post("/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                                        .andExpect(result -> {
                                                int status = result.getResponse().getStatus();
                                                if (status != HttpServletResponse.SC_OK &&
                                                                status != HttpServletResponse.SC_BAD_REQUEST) {
                                                        throw new AssertionError("Unexpected status: " + status);
                                                }
                                        });
                }
                mockMvc.perform(post("/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isTooManyRequests());
        }
}
