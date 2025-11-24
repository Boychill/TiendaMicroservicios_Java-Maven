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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Habilita Mockito sin cargar Spring Context
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService; // Inyecta los mocks aquí automáticamente

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        // Datos de prueba comunes
        registerRequest = new RegisterRequest("Juan", "Perez", "juan@test.com", "123456");
        loginRequest = new LoginRequest("juan@test.com", "123456");
        
        user = User.builder()
                .id(UUID.randomUUID())
                .firstName("Juan")
                .lastName("Perez")
                .email("juan@test.com")
                .passwordHash("encodedPassword")
                .role(Role.CLIENTE)
                .build();
    }

    @Test
    void register_Success() {
        // 1. Arrange (Preparar comportamiento de los Mocks)
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        // 2. Act (Ejecutar el método real)
        AuthResponse response = authService.register(registerRequest);

        // 3. Assert (Verificar resultados)
        assertNotNull(response);
        assertEquals("fake-jwt-token", response.token());
        assertEquals("juan@test.com", response.email());
        
        // Verificar que el repositorio fue llamado una vez
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_Fails_WhenEmailExists() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            authService.register(registerRequest);
        });

        // Verificar que NUNCA se guardó nada
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("fake-jwt-token");
        
        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("fake-jwt-token", response.token());
        
        // Verificar que se llamó al AuthenticationManager para validar credenciales
        verify(authenticationManager).authenticate(
            any(UsernamePasswordAuthenticationToken.class)
        );
    }

    @Test
    void login_Fails_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.empty());

        // Se espera que el AuthenticationManager valide antes, pero si pasa y no encuentra user:
        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
    }
}