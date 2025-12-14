package com.eventos.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Desabilita o CSRF (padrão para APIs REST)
            .csrf(csrf -> csrf.disable())
            
            // 2. IMPORTANTE: Desabilita o CORS do Spring Security no Microsserviço
            // O Gateway já está adicionando os headers, então aqui não pode ter nada.
            .cors(cors -> cors.disable()) 
            
            // 3. Libera as rotas de autenticação
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth", "/users").permitAll()
                .anyRequest().permitAll() // Deixa aberto por enquanto para facilitar testes
            );
            
        return http.build();
    }
}