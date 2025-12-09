package com.eventos.registration_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eventos.registration_service.model.Registration;

public interface RegistrationRepo extends JpaRepository<Registration, Long> {
    // Busca específica para validação e check-in
    Optional<Registration> findByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);
    
    // Lista todas as inscrições de um usuário
    List<Registration> findByUsuarioId(Long usuarioId);
}