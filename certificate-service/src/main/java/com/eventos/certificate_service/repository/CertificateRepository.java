package com.eventos.certificate_service.repository;

import com.eventos.certificate_service.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Certificate findByCode(String code);

    Optional<Certificate> findByUserIdAndEventId(Long userId, Long eventId);
    List<Certificate> findByUserId(Long userId);
}
