package com.api.service.catalogo.config;

import com.api.service.catalogo.model.Product;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

@Configuration
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    public MongoConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initIndices() {
        // 1. Índice de Texto
        TextIndexDefinition textIndex = TextIndexDefinition.builder()
                .onField("nombre", 3F)
                .onField("descripcion", 2F)
                .build();

        mongoTemplate.indexOps(Product.class).ensureIndex(textIndex);

        // 2. Índice para categorías
        mongoTemplate.indexOps(Product.class)
                .ensureIndex(new Index().on("categorias", Sort.Direction.ASC));
    }
}