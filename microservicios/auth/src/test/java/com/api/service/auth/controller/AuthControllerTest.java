package com.api.service.auth.controller;

import com.api.service.auth.AbstractIntegrationTest;
import com.api.service.auth.model.dto.AuthResponse;
import com.api.service.auth.model.dto.RegisterRequest;
import com.api.service.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@AutoConfigureMockMvc // Habilita MockMvc para hacer peticiones HTTP
public class AuthControllerTest extends AbstractIntegrationTest { // Hereda la config de Docker

    @Autowired
    private MockMvc mockMvc; // Para hacer peticiones HTTP

    @Autowired
    private UserRepository userRepository; // Repositorio real (conectado a la BD de Docker)

    @Autowired
    private ObjectMapper objectMapper; // Para convertir objetos a JSON

    // Limpiamos la BD de Docker antes de CADA test
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_ShouldCreateUser_AndReturnToken() throws Exception {
        RegisterRequest request = new RegisterRequest("Test", "Integ", "test@integ.com", "pass123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("CLIENTE"));

        // Verificamos que se guardó en la BD (real) de Testcontainers
        assertEquals(1, userRepository.count());
    }

    @Test
    void register_ShouldReturnBadRequest_WhenEmailExists() throws Exception {
        // 1. Creamos el primer usuario
        RegisterRequest request1 = new RegisterRequest("Test", "Integ", "test@integ.com", "pass123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // 2. Intentamos registrarlo de nuevo
        RegisterRequest request2 = new RegisterRequest("Otro", "User", "test@integ.com", "pass456");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest()); // Esperamos 400 Bad Request
    }

    @Test
    void testAuthentication_ShouldFailWith403_WhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/api/auth/test-auth"))
                .andExpect(status().isForbidden()); // 403 Forbidden
    }

    @Test
    void testAuthentication_ShouldSucceed_WhenValidTokenIsProvided() throws Exception {
        // 1. Registramos un usuario para obtener un token válido
        RegisterRequest request = new RegisterRequest("Test", "Token", "token@test.com", "pass123");
        String responseString = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(responseString, AuthResponse.class);
        String token = authResponse.token();

        // 2. Hacemos la petición protegida CON el token
        mockMvc.perform(get("/api/auth/test-auth")
                // Añadimos la cabecera 'Authorization: Bearer <token>'
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(
                        MockMvcResultMatchers.content().string("¡Token validado! Estás autenticado. (Java Version)"));
    }
}