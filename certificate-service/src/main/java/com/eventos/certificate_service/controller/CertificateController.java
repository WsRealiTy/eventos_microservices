package com.eventos.certificate_service.controller;

import com.eventos.certificate_service.model.Certificate;
import com.eventos.certificate_service.service.CertificateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/certificados")
public class CertificateController {

    private final CertificateService service;

    public CertificateController(CertificateService service) {
        this.service = service;
    }

    // LISTAR MEUS CERTIFICADOS (Autenticado)
    @GetMapping
    public List<Certificate> listarMeus(@RequestAttribute("userId") Long userId) {
        return service.listarPorUsuario(userId);
    }

    // EMITIR CERTIFICADO (Autenticado)
    // Body: { "eventId": 1 }
    @PostMapping
    public ResponseEntity<?> emitir(@RequestBody Map<String, Long> body, 
                                    @RequestAttribute("userId") Long userId) {
        try {
            Long eventId = body.get("eventId");
            Certificate cert = service.issueCertificate(userId, eventId);
            return ResponseEntity.ok(cert);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // VALIDAR AUTENTICIDADE (PÚBLICO - Qualquer um pode checar)
    @GetMapping("/validar/{code}")
    public ResponseEntity<?> validar(@PathVariable String code) {
        Certificate cert = service.validarPorCodigo(code);
        if (cert != null) {
            return ResponseEntity.ok(cert);
        }
        return ResponseEntity.notFound().build();
    }
}