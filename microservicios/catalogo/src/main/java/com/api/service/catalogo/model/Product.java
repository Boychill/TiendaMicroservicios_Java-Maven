package com.api.service.catalogo.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    private String id; // MongoDB usa String para IDs

    @NotBlank(message = "El nombre no puede estar vacío")
    @TextIndexed(weight = 3) // Prioridad alta para búsqueda
    private String nombre;

    @TextIndexed(weight = 2)
    private String descripcion;

    @Min(value = 0, message = "El precio no puede ser negativo")
    private Double precio;

    @PositiveOrZero(message = "El stock no puede ser negativo")
    private Integer stock;

    private List<String> categorias;

    private String imageUrl;
}