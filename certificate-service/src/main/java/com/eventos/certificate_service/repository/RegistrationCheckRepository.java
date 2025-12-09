package com.eventos.certificate_service.repository;

import com.eventos.certificate_service.model.RegistrationCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RegistrationCheckRepository extends JpaRepository<RegistrationCheck, Long> {
    Optional<RegistrationCheck> findByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);
}