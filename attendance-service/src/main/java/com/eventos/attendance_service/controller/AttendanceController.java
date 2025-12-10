package com.eventos.attendance_service.controller;

import com.eventos.attendance_service.model.Attendance;
import com.eventos.attendance_service.service.AttendanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/presencas")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @PostMapping
    public Attendance registrar(@RequestBody Attendance attendance) {
        return service.register(attendance);
    }

    @GetMapping("/evento/{eventId}")
    public List<Attendance> listarPorEvento(@PathVariable Long eventId) {
        return service.getByEvent(eventId);
    }

    @GetMapping("/usuario/{userId}")
    public List<Attendance> listarPorUsuario(@PathVariable Long userId) {
        return service.getByUser(userId);
    }

    @PostMapping("/sync")
    public List<Attendance> sincronizar(@RequestBody List<Attendance> attendances) {
       // lembrar de add dps para evitar duplicados
        return service.saveAll(attendances); 
    }
}
