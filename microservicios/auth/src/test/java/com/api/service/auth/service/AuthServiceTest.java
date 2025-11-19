package com.api.service.auth.service;

import com.api.service.auth.model.dto.AuthResponse;
import com.api.service.auth.model.dto.LoginRequest;
import com.api.service.auth.model.dto.RegisterRequest;
import com.api.service.auth.model.entity.Role;
import com.api.service.auth.model.entity.User;
import com.api.service.auth.repository.UserRepository;
import com.api.service.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test Unitario para AuthService (usando Mockito).
 * Prueba la lógica de negocio en aislamiento.
 */
@ExtendWith(MockitoExtension.class) // Habilita Mockito
class AuthServiceTest {

    // Creamos "mocks" (objetos falsos) para las dependencias
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    // Inyectamos los mocks de arriba en nuestra clase de servicio
    @InjectMocks
    private AuthService authService;

    @Test
    void register_ShouldRegisterUserSuccessfully_WhenEmailNotExists() {
        // --- 1. Arrange (Preparación) ---
        RegisterRequest request = new RegisterRequest("Test", "User", "test@user.com", "password123");
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .passwordHash("hashedPassword")
                .role(Role.CLIENTE)
                .build();

        // Definimos el comportamiento de los mocks
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("fake.jwt.token");

        // --- 2. Act (Actuación) ---
        AuthResponse response = authService.register(request);

        // --- 3. Assert (Verificación) ---
        assertNotNull(response);
        assertEquals("fake.jwt.token", response.token());
        assertEquals("CLIENTE", response.role());
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {
        // --- 1. Arrange ---
        RegisterRequest request = new RegisterRequest("Test", "User", "test@user.com", "password123");

        // Definimos el comportamiento (el email SÍ existe)
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // --- 2. Act & 3. Assert ---
        // Verificamos que se lanza la excepción correcta
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.register(request);
        });

        assertEquals("El email ya está en uso", exception.getMessage());
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsAreValid() {
        // --- 1. Arrange ---
        LoginRequest request = new LoginRequest("test@user.com", "password123");
        User userFromDb = User.builder()
                .id(UUID.randomUUID())
                .email(request.email())
                .firstName("Test")
                .lastName("User")
                .passwordHash("hashedPassword")
                .role(Role.ADMIN)
                .build();

        // Definimos el comportamiento
        when(userRepository.findByEmail(request.email().toLowerCase()))
                .thenReturn(Optional.of(userFromDb));
        when(jwtService.generateToken(userFromDb)).thenReturn("fake.admin.token");
        // Nota: No mockeamos authenticationManager.authenticate() porque devuelve void
        // y nuestro AuthService no depende de su valor de retorno.

        // --- 2. Act ---
        AuthResponse response = authService.login(request);

        // --- 3. Assert ---
        assertNotNull(response);
        assertEquals("fake.admin.token", response.token());
        assertEquals("ADMIN", response.role());
    }
}