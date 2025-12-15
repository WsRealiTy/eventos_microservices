package com.eventos.certificate_service.repository;

import com.eventos.certificate_service.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    // Método para verificar se existe check-in
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
}