package com.eventos.event_service.repository;

import com.eventos.event_service.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface EventRepository extends JpaRepository<Event, Long> {
    
}
