package com.api.service.pedidos.controller;

import com.api.service.pedidos.model.Order;
import com.api.service.pedidos.model.OrderStatus;
import com.api.service.pedidos.repository.OrderRepository;
import com.api.service.pedidos.service.OrderService; // Importamos el servicio

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pedidos")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService; // Inyectamos el servicio

    public OrderController(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo pedido (Descuenta stock)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createOrder(@RequestBody Order order, @AuthenticationPrincipal Jwt jwt) {
        try {
            // Extraer userId del token
            String userIdString = jwt.getClaim("userId");
            UUID userId = UUID.fromString(userIdString);

            // Asignar userId y delegar la lógica de stock
            order.setUserId(userId);

            Order savedOrder = orderService.createOrder(order, jwt.getTokenValue());
            return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            // Captura errores de stock o comunicación
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/mis-pedidos")
    @Operation(summary = "Ver mis pedidos (Cliente)")
    // Permitimos ADMIN ver sus pedidos personales
    @PreAuthorize("hasAnyAuthority('ROLE_CLIENTE', 'ROLE_ADMIN')")
    public ResponseEntity<List<Order>> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        String userIdString = jwt.getClaim("userId");
        UUID userId = UUID.fromString(userIdString);

        return ResponseEntity.ok(orderRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/todos")
    @Operation(summary = "Ver todos los pedidos (Admin/Despachador)")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DESPACHADOR')")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAllByOrderByCreatedAtDesc());
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Actualizar estado del pedido")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DESPACHADOR')")
    public ResponseEntity<Order> updateStatus(@PathVariable UUID id, @RequestParam OrderStatus status) {
        try {
            Order updatedOrder = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}