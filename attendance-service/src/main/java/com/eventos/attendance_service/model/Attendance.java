package com.eventos.attendance_service.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "attendances")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long eventId;
    private boolean checkedIn;
    private String timestamp;
}
