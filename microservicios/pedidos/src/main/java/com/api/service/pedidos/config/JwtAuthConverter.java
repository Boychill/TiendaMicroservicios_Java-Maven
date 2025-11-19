package com.api.service.pedidos.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 1. Extraer Rol
        String role = jwt.getClaim("role");
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }

        // 2. Extraer UserId (Añadimos esto a los 'details' del token)
        String userIdString = jwt.getClaim("userId");

        // Construimos un mapa de claims extra para pasarlo al objeto de autenticación
        Map<String, Object> claims = new HashMap<>(jwt.getClaims());
        if (userIdString != null) {
            // Nos aseguramos de que esté disponible como UUID
            claims.put("userIdUUID", UUID.fromString(userIdString));
        }

        // Retornamos el token autenticado
        // Usamos jwt.getSubject() (email) como nombre principal
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}