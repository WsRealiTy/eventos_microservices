package com.eventos.registration_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            .csrf(csrf -> csrf.disable()) // Desabilita proteção CSRF
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Sem sessão (Stateless)
            .authorizeHttpRequests(auth -> auth
                // 2. MUDANÇA AQUI:
                // Removemos a linha do "/eventos" pois este serviço não cuida disso.
                
                // Mantemos o actuator liberado para Health Check (opcional)
                .requestMatchers("/actuator/**").permitAll() 
                
                // 3. REGRA DE OURO:
                // Todas as requisições (/inscricoes) exigem autenticação.
                // Isso garante que o Token chegue no Controller.
                .anyRequest().authenticated() 
            )
            .addFilterBefore(jwtValidationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}