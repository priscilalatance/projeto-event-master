package com.eventmaster.catalog.domain.repository;

import com.eventmaster.catalog.domain.entity.Event;
import java.util.List;
import java.util.Optional;

public interface EventRepository {
    void save(Event event);
    Optional<Event> findById(String id);
    List<Event> findAll();
    List<Event> findByCategory(String category);
}
