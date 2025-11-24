package com.api.service.pedidos.service;

import com.api.service.pedidos.model.Order;
import com.api.service.pedidos.model.OrderStatus;
import com.api.service.pedidos.repository.OrderRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate; // Mantenemos privado, pero inyectado

    public OrderService(OrderRepository orderRepository, RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public Order createOrder(Order order, String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String catalogoUrl = "http://localhost:8080/api/catalogo/stock/reducir/";

        if (order.getItems() != null) {
            for (var item : order.getItems()) {
                String url = catalogoUrl + item.getProductId() + "?cantidad=" + item.getCantidad();

                try {
                    // Llamamos al RestTemplate inyectado
                    restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
                } catch (Exception e) {
                    throw new RuntimeException("Stock insuficiente o error de comunicación: " + e.getMessage());
                }
            }
        }

        order.setStatus(OrderStatus.PENDIENTE);
        if (order.getItems() != null) {
            order.getItems().forEach(item -> item.setOrder(order));
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }
}