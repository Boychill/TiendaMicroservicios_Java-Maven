package com.api.service.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    public AuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // Rutas que son públicas sin importar el método
    private final Set<String> publicPaths = Set.of(
            "/api/auth/register",
            "/api/auth/login");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        // --- LÓGICA DE FILTRADO (Idéntica a la de Kotlin) ---

        // 1. Si es una ruta pública estándar (ej. login), dejar pasar.
        if (publicPaths.contains(path)) {
            return chain.filter(exchange);
        }

        // 2. Si es GET Y la ruta empieza con /api/catalogo (ver productos), dejar
        // pasar.
        if (method == HttpMethod.GET && path.startsWith("/api/catalogo")) {
            return chain.filter(exchange);
        }

        // --- Si no es pública, validamos el token ---

        // 3. Obtener la cabecera "Authorization"
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // 4. Si no hay cabecera o no es "Bearer", rechazar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange);
        }

        // 5. Extraer y validar el token JWT
        String token = authHeader.substring(7); // "Bearer ".length()
        if (!jwtService.validateToken(token)) {
            return unauthorizedResponse(exchange);
        }

        // 6. Si el token es válido, dejar pasar la petición
        return chain.filter(exchange);
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // Orden de prioridad alto
    }
}