package com.eventos.certificate_service.controller;

import com.eventos.certificate_service.model.Certificate;
import com.eventos.certificate_service.service.CertificateService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/certificados")
public class CertificateController {

    private final CertificateService service;

    public CertificateController(CertificateService service) {
        this.service = service;
    }

    @GetMapping
    public List<Certificate> listar() {
        return service.findAll();
    }

    @GetMapping("/{code}")
    public Certificate buscar(@PathVariable String code) {
        return service.findByCode(code);
    }

    @PostMapping
    public Certificate emitir(@RequestParam Long userId, @RequestParam Long eventId) {
        return service.issueCertificate(userId, eventId);
    }
}
