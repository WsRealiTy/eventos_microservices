package com.eventos.registration_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registrations")
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long eventoId;
    private LocalDateTime dataInscricao;
    private boolean presente;

    // --- CONSTRUTORES ---
    public Registration() {}

    public Registration(Long userId, Long eventoId, LocalDateTime dataInscricao, boolean presente) {
        this.userId = userId;
        this.eventoId = eventoId;
        this.dataInscricao = dataInscricao;
        this.presente = presente;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }

    public LocalDateTime getDataInscricao() { return dataInscricao; }
    public void setDataInscricao(LocalDateTime dataInscricao) { this.dataInscricao = dataInscricao; }

    public boolean isPresente() { return presente; }
    public void setPresente(boolean presente) { this.presente = presente; }
}