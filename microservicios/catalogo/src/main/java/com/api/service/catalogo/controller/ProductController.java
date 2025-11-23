package com.api.service.catalogo.controller;

import com.api.service.catalogo.model.Product;
import com.api.service.catalogo.repository.ProductRepository;
import com.api.service.catalogo.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final FileUploadService fileUploadService;

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
        // Asumiendo que tienes este método en tu repositorio
        // return ResponseEntity.ok(productRepository.searchByText(query));
        // Si no existe, comenta la línea anterior o impleméntalo.
        return ResponseEntity.ok(List.of()); // Placeholder para que compile si no tienes el método
    }

    @GetMapping("/categoria/{categoria}")
    @Operation(summary = "Buscar por categoría", security = {})
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String categoria) {
        // Asumiendo que tienes este método en tu repositorio
        // return
        // ResponseEntity.ok(productRepository.findByCategoriasContains(categoria));
        return ResponseEntity.ok(List.of()); // Placeholder
    }

    // --- PROTEGIDOS ---

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @Operation(summary = "Crear producto con imagen")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTOR_INVENTARIO')")
    public ResponseEntity<Product> createProduct(
            @RequestPart("product") @Valid Product product,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {

        if (image != null && !image.isEmpty()) {
            String imageUrl = fileUploadService.uploadFile(image);
            product.setImageUrl(imageUrl);
        }
        product.setId(null);
        return new ResponseEntity<>(productRepository.save(product), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto completo")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTOR_INVENTARIO')")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @Valid @RequestBody Product productDetails) {
        return productRepository.findById(id)
                .map(existingProduct -> {
                    existingProduct.setNombre(productDetails.getNombre());
                    existingProduct.setDescripcion(productDetails.getDescripcion());
                    existingProduct.setPrecio(productDetails.getPrecio());
                    existingProduct.setStock(productDetails.getStock()); // Actualiza stock también
                    existingProduct.setCategorias(productDetails.getCategorias());
                    // Solo actualizamos imagen si viene una URL nueva en el JSON
                    if (productDetails.getImageUrl() != null) {
                        existingProduct.setImageUrl(productDetails.getImageUrl());
                    }
                    return ResponseEntity.ok(productRepository.save(existingProduct));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- CORRECCIÓN: Manejo de IOException dentro de la lambda ---
    @PostMapping(value = "/{id}/actualizar", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @Operation(summary = "Actualizar producto con imagen nueva")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTOR_INVENTARIO')")
    public ResponseEntity<Product> updateProductWithImage(
            @PathVariable String id,
            @RequestPart("product") @Valid Product productDetails,
            @RequestPart(value = "image", required = false) MultipartFile image) { // Quitamos throws IOException aquí
                                                                                   // porque se maneja dentro

        return productRepository.findById(id)
                .map(existingProduct -> {
                    existingProduct.setNombre(productDetails.getNombre());
                    existingProduct.setDescripcion(productDetails.getDescripcion());
                    existingProduct.setPrecio(productDetails.getPrecio());
                    existingProduct.setStock(productDetails.getStock());
                    existingProduct.setCategorias(productDetails.getCategorias());

                    // Lógica segura para la imagen dentro de lambda
                    if (image != null && !image.isEmpty()) {
                        try {
                            String imageUrl = fileUploadService.uploadFile(image);
                            existingProduct.setImageUrl(imageUrl);
                        } catch (IOException e) {
                            // Envolvemos la excepción verificada en una no verificada
                            throw new RuntimeException("Error al subir la imagen: " + e.getMessage(), e);
                        }
                    }

                    return ResponseEntity.ok(productRepository.save(existingProduct));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/stock/{id}")
    @Operation(summary = "Actualizar stock manualmente")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTOR_INVENTARIO', 'ROLE_REPONEDOR')")
    public ResponseEntity<Product> updateStock(@PathVariable String id, @RequestParam @Min(0) Integer stock) {
        return productRepository.findById(id)
                .map(existingProduct -> {
                    existingProduct.setStock(stock);
                    return ResponseEntity.ok(productRepository.save(existingProduct));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/stock/reducir/{id}")
    @Operation(summary = "Reducir stock por compra")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> reduceStock(@PathVariable String id, @RequestParam Integer cantidad) {
        return productRepository.findById(id)
                .map(product -> {
                    if (product.getStock() < cantidad) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Stock insuficiente");
                    }
                    product.setStock(product.getStock() - cantidad);
                    productRepository.save(product);
                    return ResponseEntity.ok("Stock actualizado");
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Producto no encontrado"));
    }
}