package com.eventos.registration_service.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventos.registration_service.model.Registration;
import com.eventos.registration_service.repository.RegistrationRepo;

@RestController
@RequestMapping("/inscricoes") // Mudei para português conforme o checklist
public class RegistrationController {

    private final RegistrationRepo repo;

    public RegistrationController(RegistrationRepo repo) {
        this.repo = repo;
    }

    // LISTAR MINHAS INSCRIÇÕES
    @GetMapping
    public List<Registration> minhasInscricoes(@RequestAttribute("userId") Long usuarioId) {
        // O userId vem automaticamente do Token JWT (Segurança)
        return repo.findByUsuarioId(usuarioId);
    }

    // NOVA INSCRIÇÃO
    @PostMapping
    public ResponseEntity<?> inscrever(@RequestBody Registration dados, 
                                       @RequestAttribute("userId") Long usuarioId) {
        
        // 1. Verifica se já está inscrito
        if (repo.findByUsuarioIdAndEventoId(usuarioId, dados.getEventoId()).isPresent()) {
            return ResponseEntity.badRequest().body("Você já está inscrito neste evento!");
        }

        // 2. Prepara os dados (Força o ID do usuário logado)
        dados.setUsuarioId(usuarioId);
        dados.setDataInscricao(LocalDateTime.now());
        dados.setPresente(false);

        Registration salvo = repo.save(dados);
        return ResponseEntity.ok(salvo);
    }

    // CHECK-IN (PRESENÇA)
    @PostMapping("/presenca")
    public ResponseEntity<?> registrarPresenca(@RequestBody Registration dados, 
                                               @RequestAttribute("userId") Long usuarioId) {
        // Busca a inscrição
        Optional<Registration> inscricao = repo.findByUsuarioIdAndEventoId(usuarioId, dados.getEventoId());

        if (inscricao.isEmpty()) {
            return ResponseEntity.badRequest().body("Inscrição não encontrada. Inscreva-se primeiro.");
        }

        Registration reg = inscricao.get();
        reg.setPresente(true); // Marca presença
        repo.save(reg);
        
        return ResponseEntity.ok("Check-in realizado com sucesso!");
    }
}