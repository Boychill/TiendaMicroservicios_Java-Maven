package com.api.service.catalogo.controller;

import com.api.service.catalogo.model.Product;
import com.api.service.catalogo.repository.ProductRepository;
import com.api.service.catalogo.service.FileUploadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private ProductController productController;

    private ObjectMapper objectMapper;
    private Product product;

    @BeforeEach
    void setUp() {
        // Configuramos MockMvc en modo standalone para aislar el controlador
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
        objectMapper = new ObjectMapper();

        // Datos de prueba
        product = Product.builder()
                .id("prod-1")
                .nombre("Laptop Gamer")
                .descripcion("Potente laptop")
                .precio(1500.0)
                .stock(10)
                .categorias(List.of("Tecnologia", "Computacion"))
                .imageUrl("http://img.com/default.jpg")
                .build();
    }

    @Test
    void getAllProducts_ShouldReturnList() throws Exception {
        // Arrange
        when(productRepository.findAll()).thenReturn(Arrays.asList(product));

        // Act & Assert
        mockMvc.perform(get("/api/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Laptop Gamer"))
                .andExpect(jsonPath("$[0].id").value("prod-1"));

        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getProductById_WhenExists_ShouldReturnProduct() throws Exception {
        // Arrange
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        // Act & Assert
        mockMvc.perform(get("/api/catalogo/{id}", "prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Laptop Gamer"));
    }

    @Test
    void getProductById_WhenNotExists_ShouldReturn404() throws Exception {
        // Arrange
        when(productRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/catalogo/{id}", "non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_WithImage_ShouldUploadAndSave() throws Exception {
        // Arrange: Preparamos el archivo y el JSON del producto
        MockMultipartFile imageFile = new MockMultipartFile(
                "image", "foto.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-content".getBytes());

        MockMultipartFile productJson = new MockMultipartFile(
                "product", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(product));

        // Simulamos la respuesta de Cloudinary y del Repositorio
        when(fileUploadService.uploadFile(any())).thenReturn("http://cloudinary.com/foto-nueva.jpg");
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Hacemos un POST multipart
        mockMvc.perform(multipart("/api/catalogo")
                .file(imageFile)
                .file(productJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value("http://cloudinary.com/foto-nueva.jpg"));

        // Assert: Verificamos que se llamó al servicio de subida
        verify(fileUploadService, times(1)).uploadFile(any());
    }

    @Test
    void updateProduct_ShouldUpdateFields() throws Exception {
        // Arrange
        Product updatedDetails = Product.builder()
                .nombre("Laptop Gamer Pro")
                .precio(2000.0)
                .stock(5)
                .build();

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(updatedDetails);

        // Act
        mockMvc.perform(put("/api/catalogo/{id}", "prod-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Laptop Gamer Pro"));
    }

    @Test
    void deleteProduct_WhenExists_ShouldReturnNoContent() throws Exception {
        // Arrange
        when(productRepository.existsById("prod-1")).thenReturn(true);
        doNothing().when(productRepository).deleteById("prod-1");

        // Act & Assert
        mockMvc.perform(delete("/api/catalogo/{id}", "prod-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void reduceStock_WhenSufficient_ShouldReduce() throws Exception {
        // Arrange
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        // Act
        mockMvc.perform(put("/api/catalogo/stock/reducir/{id}", "prod-1")
                .param("cantidad", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string("Stock actualizado"));

        // Assert: El stock original era 10, reducimos 5 -> debe quedar en 5
        // Verificamos que se guardó el producto (podríamos capturar el argumento para
        // ser más precisos)
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void reduceStock_WhenInsufficient_ShouldReturnBadRequest() throws Exception {
        // Arrange: Stock es 10, pedimos 20
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        // Act
        mockMvc.perform(put("/api/catalogo/stock/reducir/{id}", "prod-1")
                .param("cantidad", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Stock insuficiente"));

        // Assert: No se debe llamar a save
        verify(productRepository, never()).save(any(Product.class));
    }
}