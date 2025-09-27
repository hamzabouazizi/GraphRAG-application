package com.tanit.cto.user_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanit.cto.user_management.model.AuthRequest;
import com.tanit.cto.user_management.model.AuthResponse;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void signup_createsUserSuccessfully() throws Exception {
                AuthRequest request = new AuthRequest("newuser@example.com", "password123");

                mockMvc.perform(post("/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("newuser@example.com"));
        }

        @Test
        void signup_duplicateUser_returnsBadRequest() throws Exception {
                AuthRequest request = new AuthRequest("duplicate@example.com", "password123");

                mockMvc.perform(post("/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void login_returnsJwtToken() throws Exception {
                AuthRequest signup = new AuthRequest("loginuser@example.com", "password123");
                mockMvc.perform(post("/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signup)))
                                .andExpect(status().isOk());

                AuthRequest login = new AuthRequest("loginuser@example.com", "password123");
                mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").exists());
        }

        @Test
        void signupLoginProfile_EndToEndFlow() throws Exception {
                String email = "endtoend@example.com";
                String password = "password123";

                AuthRequest signup = new AuthRequest(email, password);
                mockMvc.perform(post("/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signup)))
                                .andExpect(status().isOk());

                MvcResult loginResult = mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signup)))
                                .andExpect(status().isOk())
                                .andReturn();

                String json = loginResult.getResponse().getContentAsString();
                AuthResponse authResponse = objectMapper.readValue(json, AuthResponse.class);
                String token = authResponse.getToken();

                mockMvc.perform(get("/profile")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value(email));
        }

        @Test
        void profile_requiresAuthentication() throws Exception {
                mockMvc.perform(get("/profile"))
                                .andExpect(status().isUnauthorized());
        }
}
