package com.api.service.pedidos.repository;

import com.api.service.pedidos.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    // Buscar mis pedidos (ordenados por fecha)
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Buscar todos (para admin)
    List<Order> findAllByOrderByCreatedAtDesc();
}