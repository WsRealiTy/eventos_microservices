package com.eventos.registration_service.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;
    private Long eventoId;

    private boolean presente = false;
}
