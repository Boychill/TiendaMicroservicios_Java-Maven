package com.api.service.pedidos.service;

import com.api.service.pedidos.model.Order;
import com.api.service.pedidos.model.OrderItem;
import com.api.service.pedidos.model.OrderStatus;
import com.api.service.pedidos.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings; // Importar esto
import org.mockito.quality.Strictness; // Importar esto
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // <--- ¡AÑADE ESTO! Hace que el test sea tolerante
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestTemplate restTemplate; // Mock del cliente HTTP

    @InjectMocks
    private OrderService orderService; // Inyecta OrderRepository y RestTemplate

    private final UUID USER_ID = UUID.randomUUID();
    private final String VALID_TOKEN = "valid.jwt.token";

    @Test
    void createOrder_ShouldSaveOrder_AndReduceStockSuccessfully() {
        // 1. Arrange (Preparación)
        OrderItem item = OrderItem.builder().productId("prod-xyz").nombre("Laptop").cantidad(1).precio(100.0).build();
        Order newOrder = Order.builder().userId(USER_ID).precioTotal(100.0).direccionEnvio("Test Address")
                .items(List.of(item)).build();

        // Comportamiento del Mock de RestTemplate: Devuelve 200 OK.
        // Aquí es donde simulamos la llamada al catálogo
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class))).thenReturn(new ResponseEntity<>("Stock actualizado", HttpStatus.OK));

        // Comportamiento del Repository: Devuelve el objeto guardado
        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);

        // 2. Act (Actuación)
        Order savedOrder = orderService.createOrder(newOrder, VALID_TOKEN);

        // 3. Assert (Verificación)
        assertNotNull(savedOrder);
        assertEquals(OrderStatus.PENDIENTE, savedOrder.getStatus());

        // Verificación de la llamada
        verify(restTemplate, times(1)).exchange(
                contains("/stock/reducir/prod-xyz?cantidad=1"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class));
    }

    @Test
    void createOrder_ShouldThrowException_WhenCatalogReturnsInsufficientStock() {
        // 1. Arrange
        OrderItem item = OrderItem.builder().productId("prod-xyz").cantidad(100).build();
        Order newOrder = Order.builder().userId(USER_ID).precioTotal(1000.0).items(List.of(item)).build();

        // Comportamiento del Mock de RestTemplate (Simulando Error HTTP 400):
        // Usamos thenThrow(new HttpClientErrorException) para simular la falla de
        // negocio
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Stock insuficiente"));

        // 2. Act & 3. Assert
        // Esperamos una RuntimeException para abortar la transacción
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(newOrder, VALID_TOKEN);
        });

        // Verificar que la excepción es lanzada y contiene el mensaje de error
        assertTrue(exception.getMessage().contains("Stock insuficiente"));

        // Verificar que NUNCA se intentó guardar el pedido
        verify(orderRepository, never()).save(any(Order.class));
    }

    // Test para simular fallo de conexión (Connection Refused)
    @Test
    void createOrder_ShouldThrowException_WhenConnectionFails() {
        // 1. Arrange
        OrderItem item = OrderItem.builder().productId("prod-xyz").cantidad(1).build();
        Order newOrder = Order.builder().userId(USER_ID).precioTotal(100.0).items(List.of(item)).build();

        // Comportamiento del Mock de RestTemplate (Simulando Fallo de Conexión):
        // Usamos ResourceAccessException para simular "Connection refused: connect"
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class))).thenThrow(new ResourceAccessException("Connection refused: connect"));

        // 2. Act & 3. Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(newOrder, VALID_TOKEN);
        });

        // Verificar que la excepción es lanzada y contiene el mensaje de error
        assertTrue(exception.getMessage().contains("Connection refused: connect"));

        // Verificar que NUNCA se intentó guardar el pedido
        verify(orderRepository, never()).save(any(Order.class));
    }

    // --- Tests de updateOrderStatus ---

    @Test
    void updateOrderStatus_ShouldChangeStatusAndSave() {
        // 1. Arrange
        UUID orderId = UUID.randomUUID();
        Order existingOrder = Order.builder()
                .id(orderId)
                .userId(USER_ID)
                .status(OrderStatus.PENDIENTE)
                .items(Collections.emptyList())
                .direccionEnvio("Test")
                .precioTotal(100.0)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(existingOrder);

        // 2. Act
        Order updatedOrder = orderService.updateOrderStatus(orderId, OrderStatus.ENVIADO);

        // 3. Assert
        assertEquals(OrderStatus.ENVIADO, updatedOrder.getStatus());
        verify(orderRepository, times(1)).save(existingOrder);
    }
}