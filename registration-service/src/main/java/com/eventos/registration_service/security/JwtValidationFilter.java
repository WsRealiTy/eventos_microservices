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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Collections;
import java.io.IOException;
import java.util.List;

@Component
public class JwtValidationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if ("/actuator/health".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        // Se não tiver token, segue o fluxo (o SecurityConfig vai bloquear se for rota privada)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer "

        try {
            // 1. Validar e Ler o Token
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userEmail = claims.getSubject();
            String role = claims.get("role", String.class); // Pega a role (ADMIN, PARTICIPANTE)

            // 2. CRÍTICO: Avisar o Spring Security que o usuário está autenticado!
            if (userEmail != null) {
                // Cria a lista de permissões (Ex: ROLE_ADMIN)
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                
                // Cria o objeto de autenticação oficial do Spring
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userEmail, null, Collections.singletonList(authority));
                
                // Injeta no contexto
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            // Opcional: Passar atributos para o controller
            request.setAttribute("userId", claims.get("id"));

        } catch (Exception e) {
            // Se o token for inválido, limpamos o contexto por segurança
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Erro de Token: " + e.getMessage());
            return; // Interrompe aqui
        }

        filterChain.doFilter(request, response);
    }
}