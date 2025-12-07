package com.eventos.certificate_service.service;

import com.eventos.certificate_service.model.Certificate;
import com.eventos.certificate_service.repository.CertificateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CertificateService {

    private final CertificateRepository repository;

    public CertificateService(CertificateRepository repository) {
        this.repository = repository;
    }

    public Certificate issueCertificate(Long userId, Long eventId) {
        Certificate certificate = new Certificate();
        certificate.setUserId(userId);
        certificate.setEventId(eventId);
        certificate.setCode(UUID.randomUUID().toString());
        certificate.setIssuedAt(LocalDateTime.now());

        return repository.save(certificate);
    }

    public Certificate findByCode(String code) {
        return repository.findByCode(code);
    }

    public List<Certificate> findAll() {
        return repository.findAll();
    }
}
