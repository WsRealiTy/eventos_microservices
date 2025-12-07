package com.eventos.registration_service.repository;

import com.eventos.registration_service.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepo extends JpaRepository<Registration, Long> {
}
