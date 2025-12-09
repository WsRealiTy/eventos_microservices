package com.eventos.event_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtValidationFilter jwtValidationFilter;

    public SecurityConfig(JwtValidationFilter jwtValidationFilter) {
        this.jwtValidationFilter = jwtValidationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Desabilita proteção CSRF (necessário para APIs)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Não guarda sessão (usa Token)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/eventos").permitAll() // Exemplo: Listar eventos é público? Se sim, deixe assim.
                .requestMatchers("/actuator/**").permitAll() // Libera health check
                .anyRequest().authenticated() // Todo o resto precisa de token
            )
            .addFilterBefore(jwtValidationFilter, UsernamePasswordAuthenticationFilter.class); // Adiciona nosso filtro JWT

        return http.build();
    }
}