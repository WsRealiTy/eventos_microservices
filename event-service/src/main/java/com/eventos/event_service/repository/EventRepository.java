package com.eventos.event_service.repository;

import com.eventos.event_service.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;


public interface EventRepository extends JpaRepository<Event, Long> {
    
}
