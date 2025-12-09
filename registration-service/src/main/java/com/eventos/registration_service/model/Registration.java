package com.eventos.registration_service.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Data
@Table(name = "registrations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"usuarioId", "eventoId"}) 
})
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long usuarioId; 

    @Column(nullable = false)
    private Long eventoId;  

    private boolean presente = false;
    
    private LocalDateTime dataInscricao = LocalDateTime.now();
}