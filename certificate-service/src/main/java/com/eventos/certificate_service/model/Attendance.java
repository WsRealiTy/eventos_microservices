package com.eventos.certificate_service.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "attendances") // Mapeia a tabela criada pelo outro serviço
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long eventId;
    private boolean checkedIn;
    private String timestamp;
}