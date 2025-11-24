package com.api.service.auth.controller;

import com.api.service.auth.model.dto.AuthResponse;
import com.api.service.auth.model.dto.LoginRequest;
import com.api.service.auth.model.dto.RegisterRequest;
import com.api.service.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

        private MockMvc mockMvc;

        @Mock
        private AuthService authService;

        @InjectMocks
        private AuthController authController;

        private ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void setUp() {
                // Configuramos MockMvc solo para este controlador (Standalone)
                // Esto evita cargar SecurityConfig, filtros, etc.
                mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        }

        @Test
        void register_ShouldReturn200_WhenValidRequest() throws Exception {
                RegisterRequest req = new RegisterRequest("Juan", "Perez", "juan@test.com", "123456");
                AuthResponse res = new AuthResponse("token123", UUID.randomUUID(), "juan@test.com", "CLIENTE");

                when(authService.register(any(RegisterRequest.class))).thenReturn(res);

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("token123"))
                                .andExpect(jsonPath("$.email").value("juan@test.com"));
        }

        @Test
        void register_ShouldReturn400_WhenEmailExists() throws Exception {
                RegisterRequest req = new RegisterRequest("Juan", "Perez", "existe@test.com", "123456");

                // Simulamos que el servicio lanza la excepción
                when(authService.register(any(RegisterRequest.class)))
                                .thenThrow(new IllegalArgumentException("El email ya existe"));

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void login_ShouldReturn200_WhenCredentialsValid() throws Exception {
                LoginRequest req = new LoginRequest("juan@test.com", "123456");
                AuthResponse res = new AuthResponse("token123", UUID.randomUUID(), "juan@test.com", "CLIENTE");

                when(authService.login(any(LoginRequest.class))).thenReturn(res);

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("token123"));
        }
}