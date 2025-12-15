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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.eventos.user_service.dto.LoginDTO;
import com.eventos.user_service.model.User;
import com.eventos.user_service.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.eventos.user_service.service.EmailClient;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
// @CrossOrigin(origins = "*") 
public class UserController {

    private final UserRepository repository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private EmailClient emailClient;

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

            // NOVO: Envia e-mail de boas-vindas
            emailClient.enviarEmail(
                salvo.getEmail(), 
                "Bem-vindo ao EventosSystem", 
                "Olá " + salvo.getName() + ", seu cadastro foi realizado com sucesso!"
            );

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

    @PutMapping("/users/{id}/senha")
    public ResponseEntity<?> atualizarSenha(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String novaSenha = payload.get("password");
            
            if (novaSenha == null || novaSenha.isEmpty()) {
                return ResponseEntity.badRequest().body("A nova senha é obrigatória.");
            }

            return repository.findById(id).map(user -> {
                user.setPassword(passwordEncoder.encode(novaSenha)); // Criptografa antes de salvar
                repository.save(user);
                return ResponseEntity.ok("Senha atualizada com sucesso!");
            }).orElse(ResponseEntity.notFound().build());
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao atualizar senha.");
        }
    }

    // 1. Endpoint para buscar dados de um usuário específico (Para Edição e Certificado)
    @GetMapping("/users/{id}")
    public ResponseEntity<User> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 2. Endpoint unificado para atualizar Perfil (Dados + Senha Opcional)
    // Substitui o antigo /users/{id}/senha se desejar, ou mantém ambos.
    @PutMapping("/users/{id}")
    public ResponseEntity<?> atualizarPerfil(@PathVariable Long id, @RequestBody User dados) {
        return repository.findById(id).map(user -> {
            // Atualiza dados cadastrais
            user.setName(dados.getName());
            user.setCpf(dados.getCpf());
            user.setEnderecoRua(dados.getEnderecoRua());
            user.setEnderecoNumero(dados.getEnderecoNumero());
            user.setEnderecoBairro(dados.getEnderecoBairro());
            user.setEnderecoCidade(dados.getEnderecoCidade());
            user.setEnderecoEstado(dados.getEnderecoEstado());

            // Atualiza senha APENAS se o usuário enviou uma nova
            if (dados.getPassword() != null && !dados.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(dados.getPassword()));
            }

            repository.save(user);
            return ResponseEntity.ok("Perfil atualizado com sucesso!");
        }).orElse(ResponseEntity.notFound().build());
    }
}