package com.eventos.attendance_service.service;

import com.eventos.attendance_service.model.Attendance;
import com.eventos.attendance_service.repository.AttendanceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRepository repository;

    public AttendanceService(AttendanceRepository repository) {
        this.repository = repository;
    }

    public Attendance register(Attendance attendance) {
        attendance.setCheckedIn(true);
        return repository.save(attendance);
    }

    public List<Attendance> getByEvent(Long eventId) {
        return repository.findByEventId(eventId);
    }

    public List<Attendance> getByUser(Long userId) {
        return repository.findByUserId(userId);
    }

    public List<Attendance> saveAll(List<Attendance> attendances) {
        return repository.saveAll(attendances);
    }
}
