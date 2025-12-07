package com.eventos.registration_service.controller;

import com.eventos.inscricao_service.model.Registration;
import com.eventos.inscricao_service.repository.RegistrationRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/registration")
public class RegistrationController {

    private final RegistrationRepo repo;

    public RegistrationController(RegistrationRepo repo) {
        this.repo = repo;
    }

    @PostMapping
    public Registration create(@RequestBody Registration registration){
        return repo.save(Registration);
    }

    @GetMapping("/{id}")
    public Registration get(@PathVariable Long id){
        return repo.findById(id).orElse(null);
    }
}
