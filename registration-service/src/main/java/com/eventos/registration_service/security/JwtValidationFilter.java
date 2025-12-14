package com.eventos.registration_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component
public class JwtValidationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                // 1. Decodifica o Token
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // 2. CORREÇÃO: Extrai o ID (número) em vez do Email (subject)
                // O UserController salva como "id" no token, então pegamos aqui.
                Integer userIdInt = claims.get("id", Integer.class);
                String userIdString = String.valueOf(userIdInt);
                
                String role = claims.get("role", String.class);
                List<SimpleGrantedAuthority> authorities = (role != null) ? 
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)) : 
                        Collections.emptyList();

                // 3. Define o ID numérico como o "Principal" da autenticação
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userIdString, // Agora auth.getName() retornará "1", "2", etc.
                        null,
                        authorities
                );
                
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                // Token inválido
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}