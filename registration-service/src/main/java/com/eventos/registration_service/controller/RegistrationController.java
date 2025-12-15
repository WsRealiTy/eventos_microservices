package com.eventos.registration_service.controller;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.eventos.registration_service.model.Registration;
import com.eventos.registration_service.repository.RegistrationRepo;
import com.eventos.registration_service.service.EmailClient;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/inscricoes")
public class RegistrationController {

    @Autowired
    private RegistrationRepo repository;

    @Autowired
    private EmailClient emailClient;

    public static class InscricaoDTO {
        public Long eventoId;
        public Long userId; 
    }

    @GetMapping
    public List<Registration> minhasInscricoes() {
        Long userId = getAuthenticatedUserId();
        return repository.findByUserId(userId);
    }

   @PostMapping
    public ResponseEntity<?> inscrever(@RequestBody InscricaoDTO dto) {
        Long userId = resolverUserId(dto.userId);

        if (repository.findByUserIdAndEventoId(userId, dto.eventoId).isPresent()) {
            return ResponseEntity.badRequest().body("Usuário já inscrito neste evento.");
        }

        Registration reg = new Registration();
        reg.setUserId(userId);
        reg.setEventoId(dto.eventoId);
        reg.setDataInscricao(LocalDateTime.now());
        reg.setPresente(false);
        
        repository.save(reg);

        // NOVO: Enviar e-mail de confirmação
        String userEmail = getUserEmailFromToken();
        if (userEmail != null) {
            emailClient.enviarEmail(userEmail, "Inscrição Confirmada", 
                "Sua inscrição no evento (ID: " + dto.eventoId + ") foi realizada com sucesso.");
        }

        return ResponseEntity.ok(reg);
    }

    // NOVO: Endpoint de Cancelamento
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        Long userId = getAuthenticatedUserId(); // Pega ID do token

        return repository.findById(id).map(reg -> {
            // Verifica se a inscrição pertence ao usuário logado (Segurança)
            if (!reg.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body("Acesso negado a esta inscrição.");
            }

            repository.delete(reg);

            // Envia e-mail de cancelamento
            String userEmail = getUserEmailFromToken();
            if (userEmail != null) {
                emailClient.enviarEmail(userEmail, "Inscrição Cancelada", 
                    "Sua inscrição no evento (ID: " + reg.getEventoId() + ") foi cancelada.");
            }

            return ResponseEntity.ok("Inscrição cancelada com sucesso.");
        }).orElse(ResponseEntity.notFound().build());
    }

    /* Endpoint específico para Check-in chamado pelo Frontend
    @PostMapping("/presenca")
    public ResponseEntity<?> registrarPresenca(@RequestBody InscricaoDTO dto) {
        Long userId = resolverUserId(dto.userId);
        
        var optReg = repository.findByUserIdAndEventoId(userId, dto.eventoId);
    
        if (optReg.isEmpty()) {
            return ResponseEntity.badRequest().body("Inscrição não encontrada para este usuário/evento.");
        }

        Registration reg = optReg.get();
        
       
        boolean jaEstavaPresente = reg.isPresente(); 

        reg.setPresente(true); 
        repository.save(reg);
        
        
        if (!jaEstavaPresente) {
            String userEmail = getUserEmailFromToken();
            if (userEmail != null) {
                emailClient.enviarEmail(userEmail, "Presença Confirmada!", 
                    "Sua presença no evento (ID: " + dto.eventoId + ") foi registrada com sucesso. Obrigado por comparecer!");
            }
        }
        
        return ResponseEntity.ok("Check-in realizado com sucesso!");
    }*/

    @GetMapping("/{id}")
    public ResponseEntity<Registration> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Métodos Auxiliares de Segurança ---

    private Long resolverUserId(Long userIdNoBody) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Verifica se é ADMIN (role vinda do JWT)
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().contains("ADMIN"));

        // Se for admin e mandou um ID, usa o ID mandado. Senão, pega do token.
        if (isAdmin && userIdNoBody != null) {
            return userIdNoBody;
        }
        
        return getAuthenticatedUserId();
    }

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Assume que o ID (subject) do token é numérico (ex: "123")
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Token inválido: ID do usuário não é numérico.");
        }
    }

    private String getUserEmailFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof String) {
            return (String) auth.getDetails();
        }
        return null; 
    }
}