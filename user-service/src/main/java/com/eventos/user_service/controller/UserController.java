package com.eventos.user_service.controller;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.eventos.user_service.dto.LoginDTO;
import com.eventos.user_service.model.User;
import com.eventos.user_service.repository.UserRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@RestController
@CrossOrigin(origins = "*") 
public class UserController {

    private final UserRepository repository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.secret}")
    private String secret;
    public UserController(UserRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/users")
    public List<User> listar() {
        return repository.findAll();
    }

    @PostMapping("/users")
    public ResponseEntity<?> criar(@RequestBody User user) {
        try {
            if (repository.findByEmail(user.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email já cadastrado");
            }

            user.setPassword(passwordEncoder.encode(user.getPassword()));
            
            if (user.getRole() == null) user.setRole("PARTICIPANTE");

            User salvo = repository.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao criar usuário: " + e.getMessage());
        }
    }

    @PostMapping("/auth")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginData) {
        try {
            Optional<User> userOp = repository.findByEmail(loginData.getEmail());

            if (userOp.isPresent()) {
                User user = userOp.get();
                
                if (passwordEncoder.matches(loginData.getPassword(), user.getPassword())) {
                    
                    Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

                    String token = Jwts.builder()
                            .setSubject(user.getEmail())
                            .claim("id", user.getId())
                            .claim("role", user.getRole())
                            .setIssuedAt(new Date())
                            .setExpiration(new Date(System.currentTimeMillis() + 3600000)) 
                            .signWith(key)
                            .compact();

                    Map<String, String> response = new HashMap<>();
                    response.put("token", token);
                    return ResponseEntity.ok(response);
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciais inválidas");
        } catch (Exception e) {
            e.printStackTrace(); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro no servidor: " + e.getMessage());
        }
    }
}