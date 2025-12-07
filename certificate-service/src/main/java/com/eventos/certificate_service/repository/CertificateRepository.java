package com.eventos.certificate_service.repository;

import com.eventos.certificate_service.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Certificate findByCode(String code);
}
