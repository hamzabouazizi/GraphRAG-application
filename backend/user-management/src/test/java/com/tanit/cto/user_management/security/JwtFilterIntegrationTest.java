package com.tanit.cto.user_management.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.transaction.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JwtFilterIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        /**
         * Profile should be blocked with invalid/expired token
         */
        @Test
        void profile_withInvalidToken_shouldReturnUnauthorized() throws Exception {
                mockMvc.perform(get("/profile")
                                .header("Authorization", "Bearer invalid.token.here"))
                                .andExpect(status().isUnauthorized());
        }

        /**
         * Profile should be accessible with valid token
         */
        @Test
        void profile_withValidToken_shouldReturnOk() throws Exception {
                // Create a fake user first
                String email = "example@example.com";
                String password = "secret";

                mockMvc.perform(post("/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"" + email + "\", \"password\":\"" + password + "\"}"))
                                .andExpect(status().isOk());

                // Log in to get a real token
                var result = mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"" + email + "\", \"password\":\"" + password + "\"}"))
                                .andExpect(status().isOk())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                String token = responseBody.replaceAll(".*:\\s*\"([^\"]+)\".*", "$1");

                // access /profile with the token
                mockMvc.perform(get("/profile")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value(email));
        }

        /**
         * End-to-end signup → login → profile flow
         */
        @Test
        void signupLoginProfile_flowWorksCorrectly() throws Exception {
                String email = "flow@example.com";
                String password = "mypassword";

                // Signup
                mockMvc.perform(post("/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"" + email + "\", \"password\":\"" + password + "\"}"))
                                .andExpect(status().isOk());

                // Login
                var loginResult = mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"" + email + "\", \"password\":\"" + password + "\"}"))
                                .andExpect(status().isOk())
                                .andReturn();

                String loginResponse = loginResult.getResponse().getContentAsString();
                String token = loginResponse.replaceAll(".*:\\s*\"([^\"]+)\".*", "$1");

                // Profile
                mockMvc.perform(get("/profile")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value(email));
        }
}
