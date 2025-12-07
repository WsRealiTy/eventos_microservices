package com.eventos.user_service.controller;

import com.eventos.user_service.model.User;
import com.eventos.user_service.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository repository;

    public UserController(UserRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<User> listar() {
        return repository.findAll();
    }

    @PostMapping
    public User criar(@RequestBody User user) {
        return repository.save(user);
    }
}
