package com.eventos.event_service.controller;

import com.eventos.event_service.model.Event;
import com.eventos.event_service.repository.EventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/eventos")
public class EventController {

    private final EventRepository repo;

    public EventController(EventRepository repo) {
        this.repo = repo;
    }

    // GET /eventos
    @GetMapping
    public List<Event> listar() {
        return repo.findAll();
    }

    // GET /eventos/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Event> consultar(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /eventos
    @PostMapping
    public ResponseEntity<Event> criar(@RequestBody Event event) {
        Event saved = repo.save(event);
        return ResponseEntity.ok(saved);
    }
}
