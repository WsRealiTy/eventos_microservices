package com.eventos.user_service.controller;

import com.eventos.user_service.dto.LoginDTO;
import com.eventos.user_service.model.User;
import com.eventos.user_service.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping
public class UserController {

    private final UserRepository repository;
    
    // Encodador de senha
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // MUDANÇA: Lendo a chave secreta do application.properties
    @Value("${jwt.secret}")
    private String secret;

    public UserController(UserRepository repository) {
        this.repository = repository;
    }

    // --- Endpoints de Usuário ---

    @GetMapping("/users")
    public List<User> listar() {
        return repository.findAll();
    }

    @PostMapping("/users")
    public ResponseEntity<User> criar(@RequestBody User user) {
        // Verifica duplicidade
        if (repository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Criptografa senha
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Role padrão
        if (user.getRole() == null) user.setRole("PARTICIPANTE");

        User salvo = repository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    // --- Endpoint de Autenticação ---

    @PostMapping("/auth")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginData) {
        Optional<User> userOp = repository.findByEmail(loginData.getEmail());

        if (userOp.isPresent()) {
            User user = userOp.get();
            
            // Valida senha
            if (passwordEncoder.matches(loginData.getPassword(), user.getPassword())) {
                
                // MUDANÇA: Gera o Token usando a chave fixa (convertida para Bytes)
                String token = Jwts.builder()
                        .setSubject(user.getEmail())
                        .claim("id", user.getId())
                        .claim("role", user.getRole())
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hora
                        .signWith(Keys.hmacShaKeyFor(secret.getBytes())) // Assina com o segredo compartilhado
                        .compact();

                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                return ResponseEntity.ok(response);
            }
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuário ou senha inválidos");
    }
}