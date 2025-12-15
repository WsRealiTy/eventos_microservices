package com.eventos.certificate_service.service;

import com.eventos.certificate_service.model.Certificate;
import com.eventos.certificate_service.repository.AttendanceRepository; // NOVO IMPORT
import com.eventos.certificate_service.repository.CertificateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CertificateService {

    private final CertificateRepository repository;
    private final AttendanceRepository attendanceRepository; // Repositório novo

    // Injeção de dependência atualizada
    public CertificateService(CertificateRepository repository, AttendanceRepository attendanceRepository) {
        this.repository = repository;
        this.attendanceRepository = attendanceRepository;
    }

    public Certificate issueCertificate(Long userId, Long eventId) {
        // 1. Verifica se já emitiu certificado para esse evento (Evitar duplicidade)
        if (repository.findByUserIdAndEventId(userId, eventId).isPresent()) {
            throw new RuntimeException("Certificado já emitido para este evento.");
        }

        // 2. VERIFICAÇÃO DE OURO: O usuário estava lá?
        // CORREÇÃO: Verifica na tabela de attendances (Attendance Service)
        boolean estevePresente = attendanceRepository.existsByUserIdAndEventId(userId, eventId);

        if (!estevePresente) {
            throw new RuntimeException("Certificado negado: O participante não tem presença confirmada (Check-in).");
        }

        // 3. Tudo certo? Emite!
        Certificate certificate = new Certificate();
        certificate.setUserId(userId);
        certificate.setEventId(eventId);
        certificate.setCode(UUID.randomUUID().toString());
        certificate.setIssuedAt(LocalDateTime.now());

        return repository.save(certificate);
    }

    public List<Certificate> listarPorUsuario(Long userId) {
        return repository.findByUserId(userId);
    }
    
    public Certificate validarPorCodigo(String code) {
        return repository.findByCode(code);
    }
}