package com.eventos.certificate_service.service;

import com.eventos.certificate_service.model.Certificate;
import com.eventos.certificate_service.model.RegistrationCheck;
import com.eventos.certificate_service.repository.CertificateRepository;
import com.eventos.certificate_service.repository.RegistrationCheckRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CertificateService {

    private final CertificateRepository repository;
    private final RegistrationCheckRepository checkRepository;

    public CertificateService(CertificateRepository repository, RegistrationCheckRepository checkRepository) {
        this.repository = repository;
        this.checkRepository = checkRepository;
    }

    public Certificate issueCertificate(Long userId, Long eventId) {
        // 1. Verifica se já emitiu certificado para esse evento (Evitar duplicidade)
        if (repository.findByUserIdAndEventId(userId, eventId).isPresent()) {
            throw new RuntimeException("Certificado já emitido para este evento.");
        }

        // 2. VERIFICAÇÃO DE OURO: O usuário estava lá?
        RegistrationCheck inscricao = checkRepository.findByUsuarioIdAndEventoId(userId, eventId)
                .orElseThrow(() -> new RuntimeException("Inscrição não encontrada."));

        if (!inscricao.isPresente()) {
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
    
    // Para validação pública
    public Certificate validarPorCodigo(String code) {
        return repository.findByCode(code);
    }
}