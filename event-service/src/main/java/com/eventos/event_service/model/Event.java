package com.eventos.event_service.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "events")
public class Event {
    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    private String descricao;
    private String local;
    private String data;
    private Integer duracaoMin;
    private String crtificadoTempalte;

}
