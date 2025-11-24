package com.api.service.catalogo.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductModelTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void product_ShouldBeValid_WhenAllFieldsCorrect() {
        Product product = Product.builder()
                .nombre("Monitor")
                .descripcion("Monitor 4k")
                .precio(300.0)
                .stock(10)
                .build();

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertTrue(violations.isEmpty(), "El producto debería ser válido");
    }

    @Test
    void product_ShouldFail_WhenNameIsEmpty() {
        Product product = Product.builder()
                .nombre("") // Inválido
                .precio(100.0)
                .stock(10)
                .build();

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertFalse(violations.isEmpty(), "Debe fallar si el nombre está vacío");

        // Opcional: Verificar el mensaje específico
        boolean hasNameError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("nombre"));
        assertTrue(hasNameError);
    }

    @Test
    void product_ShouldFail_WhenPriceIsNegative() {
        Product product = Product.builder()
                .nombre("Mouse")
                .precio(-50.0) // Inválido
                .stock(10)
                .build();

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertFalse(violations.isEmpty(), "Debe fallar si el precio es negativo");
    }

    @Test
    void product_ShouldFail_WhenStockIsNegative() {
        Product product = Product.builder()
                .nombre("Teclado")
                .precio(50.0)
                .stock(-1) // Inválido (usaste @PositiveOrZero)
                .build();

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertFalse(violations.isEmpty(), "Debe fallar si el stock es negativo");
    }
}