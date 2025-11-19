package com.api.service.catalogo.controller;

import com.api.service.catalogo.AbstractIntegrationTest;
import com.api.service.catalogo.model.Product;
import com.api.service.catalogo.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class ProductControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    // --- TEST DE UPDATE (ACTUALIZAR) ---
    @Test
    void updateProduct_ShouldUpdateAndReturn200_WhenUserIsAdmin() throws Exception {
        // 1. Arrange: Creamos un producto inicial en la BD
        Product savedProduct = productRepository.save(Product.builder()
                .nombre("Laptop Vieja")
                .descripcion("Modelo 2020")
                .precio(500.0)
                .stock(10)
                .categorias(List.of("tech"))
                .build());

        // 2. Arrange: Preparamos los datos actualizados
        Product updatePayload = Product.builder()
                .nombre("Laptop Nueva Pro") // Cambiamos el nombre
                .descripcion("Modelo 2025")
                .precio(1200.0) // Cambiamos el precio
                .stock(20)
                .categorias(List.of("tech", "pro"))
                .build();

        // 3. Act & Assert: Hacemos el PUT con token de ADMIN
        mockMvc.perform(put("/api/catalogo/" + savedProduct.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatePayload))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))) // Simula Token Admin
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Laptop Nueva Pro"))
                .andExpect(jsonPath("$.precio").value(1200.0));
    }

    @Test
    void updateProduct_ShouldFail403_WhenUserIsCliente() throws Exception {
        // 1. Guardamos producto
        Product savedProduct = productRepository.save(Product.builder()
                .nombre("Laptop")
                .precio(500.0)
                .stock(10)
                .build());

        // 2. Payload
        Product updatePayload = Product.builder().nombre("Hacker").precio(0.0).build();

        // 3. Intentamos actualizar con rol CLIENTE
        mockMvc.perform(put("/api/catalogo/" + savedProduct.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatePayload))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden()); // Debe fallar
    }

    @Test
    void createProduct_ShouldReturn201() throws Exception {
        Product newProduct = Product.builder()
                .nombre("Mouse Gamer")
                .precio(50.0)
                .stock(100)
                .build();

        mockMvc.perform(post("/api/catalogo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }
}