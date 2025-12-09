package com.eventos.certificate_service.model;

import jakarta.persistence.*;
import lombok.Data; 

@Entity
@Data
@Table(name = "registrations")
public class RegistrationCheck {
    @Id
    private Long id;
    
    private Long usuarioId;
    private Long eventoId;
    
    @Column(name = "presente")
    private boolean presente;

    // --- MÉTODOS MANUAIS ---

    public boolean isPresente() {
        return presente;
    }

    public void setPresente(boolean presente) {
        this.presente = presente;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getEventoId() {
        return eventoId;
    }
}