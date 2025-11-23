package com.api.service.pedidos.controller;

import com.api.service.pedidos.model.Order;
import com.api.service.pedidos.model.OrderItem;
import com.api.service.pedidos.model.OrderStatus;
import com.api.service.pedidos.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate; // Import necesario para llamar a Catalogo

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pedidos")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate; // Cliente HTTP para llamar a otros servicios

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
        this.restTemplate = new RestTemplate(); // Inicializamos RestTemplate
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo pedido (Descuenta stock)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createOrder(@RequestBody Order order, @AuthenticationPrincipal Jwt jwt) {

        // --- 1. LÓGICA DE STOCK (Conexión con Catálogo) ---
        String token = jwt.getTokenValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token); // Reenviamos el token del usuario
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // URL base del Gateway para llegar al servicio de catálogo
        // Asumimos que el Gateway corre en localhost:8080
        String catalogoBaseUrl = "http://localhost:8080/api/catalogo/stock/reducir/";

        try {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    // Llamamos a PUT /api/catalogo/stock/reducir/{id}?cantidad=X
                    String url = catalogoBaseUrl + item.getProductId() + "?cantidad=" + item.getCantidad();

                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

                    if (!response.getStatusCode().is2xxSuccessful()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Error al descontar stock: " + response.getBody());
                    }
                }
            }
        } catch (Exception e) {
            // Si falla la conexión o el stock es insuficiente, abortamos el pedido
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al procesar el stock: " + e.getMessage());
        }

        // --- 2. CREACIÓN DEL PEDIDO (Si el stock se descontó bien) ---
        String userIdString = jwt.getClaim("userId");
        UUID userId = UUID.fromString(userIdString);

        // Configuramos datos automáticos
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDIENTE);
        order.setCreatedAt(java.time.LocalDateTime.now());
        order.setId(null); // Generar nuevo ID

        // Relación bidireccional para JPA
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
    @Operation(summary = "Ver mis pedidos (Cliente)")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public ResponseEntity<List<Order>> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        String userIdString = jwt.getClaim("userId");
        UUID userId = UUID.fromString(userIdString);

        // Al usar FetchType.EAGER en la entidad Order, los items vienen incluidos
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