package com.api.service.pedidos.controller;

import com.api.service.pedidos.model.Order;
import com.api.service.pedidos.model.OrderItem;
import com.api.service.pedidos.model.OrderStatus;
import com.api.service.pedidos.repository.OrderRepository;
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

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo pedido")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> createOrder(@RequestBody Order order, @AuthenticationPrincipal Jwt jwt) {
        // Extraer userId del token
        String userIdString = jwt.getClaim("userId");
        UUID userId = UUID.fromString(userIdString);

        // Construir el pedido (ignorando datos que el usuario no debe poner)
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDIENTE);
        order.setCreatedAt(java.time.LocalDateTime.now());
        order.setId(null); // Generar nuevo ID

        // Asignar la relación inversa para los items
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setId(null);
                item.setOrder(order);
            }
        }

        Order savedOrder = orderRepository.save(order);
        return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
    }

    @GetMapping("/mis-pedidos")
    @Operation(summary = "Ver mis pedidos")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
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
        return orderRepository.findById(id)
                .map(order -> {
                    order.setStatus(status);
                    return ResponseEntity.ok(orderRepository.save(order));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}