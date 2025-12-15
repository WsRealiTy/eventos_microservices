package com.eventos.attendance_service.controller;

import com.eventos.attendance_service.model.Attendance;
import com.eventos.attendance_service.service.AttendanceService;
import com.eventos.attendance_service.service.EmailClient; // Importe o EmailClient que você copiou
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/presencas")
public class AttendanceController {

    private final AttendanceService service;
    private final EmailClient emailClient; // Injeção do cliente de email

    public AttendanceController(AttendanceService service, EmailClient emailClient) {
        this.service = service;
        this.emailClient = emailClient;
    }

    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Attendance attendance) {
        // 1. Verifica se já existe check-in para este usuário neste evento
        // (Você precisará garantir que o service tenha um método para buscar por User e Evento)
        // Por enquanto, vamos assumir que o service.save() cuida disso ou validamos aqui
        List<Attendance> existentes = service.getByUser(attendance.getUserId());
        boolean jaFezCheckin = existentes.stream()
                .anyMatch(a -> a.getEventId().equals(attendance.getEventId()));

        if (jaFezCheckin) {
            return ResponseEntity.badRequest().body("Check-in já realizado para este evento.");
        }

        // 2. Preenche dados faltantes
        attendance.setCheckedIn(true);
        attendance.setTimestamp(LocalDateTime.now().toString());

        // 3. Salva
        Attendance salvo = service.register(attendance);

        // 4. Envia E-mail
        try {
            String userEmail = getUserEmailFromToken();
            if (userEmail != null) {
                emailClient.enviarEmail(userEmail, "Presença Confirmada!",
                        "Sua presença no evento (ID: " + attendance.getEventId() + ") foi registrada.");
            }
        } catch (Exception e) {
            System.err.println("Erro ao enviar e-mail de presença: " + e.getMessage());
            // Não falhamos a requisição se o e-mail falhar, pois o check-in foi salvo
        }

        return ResponseEntity.ok(salvo);
    }

    @GetMapping("/evento/{eventId}")
    public List<Attendance> listarPorEvento(@PathVariable Long eventId) {
        return service.getByEvent(eventId);
    }

    @GetMapping("/usuario/{userId}")
    public List<Attendance> listarPorUsuario(@PathVariable Long userId) {
        return service.getByUser(userId);
    }

    @PostMapping("/sync")
    public List<Attendance> sincronizar(@RequestBody List<Attendance> attendances) {
        // Idealmente, filtrar duplicados aqui também
        return service.saveAll(attendances);
    }

    // Método auxiliar para pegar e-mail do token
    private String getUserEmailFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof String) {
            return (String) auth.getDetails();
        }
        // Fallback simples se o details não for string
        if (auth != null) {
             return auth.getName(); // Em alguns casos o e-mail é o principal
        }
        return null;
    }
}