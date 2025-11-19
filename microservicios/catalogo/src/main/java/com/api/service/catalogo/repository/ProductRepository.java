package com.api.service.catalogo.repository;

import com.api.service.catalogo.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    // Búsqueda de texto completo (definida en el modelo con @TextIndexed)
    @Query("{ $text: { $search: ?0 } }")
    List<Product> searchByText(String query);

    List<Product> findByCategoriasContains(String categoria);
}