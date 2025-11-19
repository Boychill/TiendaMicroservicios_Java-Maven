package com.api.service.catalogo.controller;

import com.api.service.catalogo.model.Product;
import com.api.service.catalogo.repository.ProductRepository;
import com.api.service.catalogo.service.FileUploadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/catalogo")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductRepository productRepository;
    private final FileUploadService fileUploadService; // Inyectamos el servicio

    public ProductController(ProductRepository productRepository, FileUploadService fileUploadService) {
        this.productRepository = productRepository;
        this.fileUploadService = fileUploadService;
    }

    // --- PÚBLICOS ---

    @GetMapping
    @Operation(summary = "Listar todos los productos", security = {})
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID", security = {})
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar por texto", security = {})
    public ResponseEntity<List<Product>> searchProducts(@RequestParam("q") String query) {
        return ResponseEntity.ok(productRepository.searchByText(query));
    }

    @GetMapping("/categoria/{categoria}")
    @Operation(summary = "Buscar por categoría", security = {})
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String categoria) {
        return ResponseEntity.ok(productRepository.findByCategoriasContains(categoria));
    }

    // --- PROTEGIDOS ---

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }) // Acepta Multipart
    @Operation(summary = "Crear producto con imagen")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTOR_INVENTARIO')")
    public ResponseEntity<Product> createProduct(
            @RequestPart("product") @Valid Product product, // Datos JSON
            @RequestPart(value = "image", required = false) MultipartFile image // Archivo (opcional)
    ) throws IOException {

        // 1. Si enviaron una imagen, la subimos
        if (image != null && !image.isEmpty()) {
            String imageUrl = fileUploadService.uploadFile(image);
            product.setImageUrl(imageUrl);
        }

        // 2. Guardamos en MongoDB
        product.setId(null);
        Product savedProduct = productRepository.save(product);
        return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTOR_INVENTARIO')")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @Valid @RequestBody Product productDetails) {
        return productRepository.findById(id)
                .map(existingProduct -> {
                    existingProduct.setNombre(productDetails.getNombre());
                    existingProduct.setDescripcion(productDetails.getDescripcion());
                    existingProduct.setPrecio(productDetails.getPrecio());
                    existingProduct.setStock(productDetails.getStock());
                    existingProduct.setCategorias(productDetails.getCategorias());
                    existingProduct.setImageUrl(productDetails.getImageUrl());
                    return ResponseEntity.ok(productRepository.save(existingProduct));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTOR_INVENTARIO')")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        return productRepository.findById(id)
                .map(product -> {
                    productRepository.delete(product);
                    return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/stock/{id}")
    @Operation(summary = "Actualizar stock")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTOR_INVENTARIO', 'ROLE_REPONEDOR')")
    public ResponseEntity<Product> updateStock(@PathVariable String id, @RequestParam @Min(0) Integer stock) {
        return productRepository.findById(id)
                .map(existingProduct -> {
                    existingProduct.setStock(stock);
                    return ResponseEntity.ok(productRepository.save(existingProduct));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}