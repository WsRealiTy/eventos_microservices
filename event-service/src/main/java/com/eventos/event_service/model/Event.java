package com.eventos.event_service.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "events")
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String titulo;
    private String descricao;
    private String local;
    private String data; // Sugestão: Use LocalDateTime no futuro para facilitar cálculos
    private Integer duracaoMin;
    
    @Column(name = "certificado_template") // Nome da coluna no banco
    private String certificadoTemplate; // Nome corrigido na classe
}