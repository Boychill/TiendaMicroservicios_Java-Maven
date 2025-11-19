package com.api.service.pedidos.controller;

import com.api.service.pedidos.AbstractIntegrationTest;
import com.api.service.pedidos.model.Order;
import com.api.service.pedidos.model.OrderItem;
import com.api.service.pedidos.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class OrderControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    // --- Tests de Creación ---

    @Test
    void createOrder_ShouldReturn201_WhenUserIsAuthenticated() throws Exception {
        // 1. Crear un pedido de prueba
        OrderItem item = OrderItem.builder()
                .productId("prod-123")
                .nombre("Laptop")
                .cantidad(1)
                .precio(1000.0)
                .build();

        Order newOrder = Order.builder()
                .precioTotal(1000.0)
                .direccionEnvio("Calle Falsa 123")
                .items(List.of(item))
                .build();

        // Simular el UUID del usuario en el token
        String userId = UUID.randomUUID().toString();

        // 2. Ejecutar POST
        mockMvc.perform(post("/api/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newOrder))
                .with(jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))
                        .jwt(jwt -> jwt.claim("userId", userId)))) // ¡Importante! Inyectamos el userId
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.userId").value(userId)); // Verificar que se usó el ID del token

        // 3. Verificar BD
        assertEquals(1, orderRepository.count());
    }

    // --- Tests de Consulta (Mis Pedidos) ---

    @Test
    void getMyOrders_ShouldReturnOnlyMyOrders() throws Exception {
        UUID myId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        // Guardar un pedido mío
        createOrderInDb(myId, "Laptop");
        // Guardar un pedido de otro
        createOrderInDb(otherId, "Mouse");

        // Ejecutar GET /mis-pedidos con MI token
        mockMvc.perform(get("/api/pedidos/mis-pedidos")
                .with(jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))
                        .jwt(jwt -> jwt.claim("userId", myId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // Solo debo ver 1
                .andExpect(jsonPath("$[0].items[0].nombre").value("Laptop"));
    }

    // --- Tests de Admin (Todos los pedidos) ---

    @Test
    void getAllOrders_ShouldReturnAll_WhenUserIsAdmin() throws Exception {
        createOrderInDb(UUID.randomUUID(), "Laptop");
        createOrderInDb(UUID.randomUUID(), "Mouse");

        // Ejecutar GET /todos con token ADMIN
        mockMvc.perform(get("/api/pedidos/todos")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))); // El admin ve los 2
    }

    @Test
    void getAllOrders_ShouldFail403_WhenUserIsCliente() throws Exception {
        mockMvc.perform(get("/api/pedidos/todos")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
    }

    // --- Helper ---
    private void createOrderInDb(UUID userId, String productName) {
        OrderItem item = OrderItem.builder()
                .productId("p-1")
                .nombre(productName)
                .cantidad(1)
                .precio(100.0)
                .build();

        Order order = Order.builder()
                .userId(userId)
                .precioTotal(100.0)
                .direccionEnvio("Dir")
                .items(List.of(item))
                .build();

        item.setOrder(order); // Relación bidireccional
        orderRepository.save(order);
    }
}