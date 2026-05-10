package com.eventmaster.catalog.domain.strategy;

import com.eventmaster.catalog.domain.entity.Event;
import com.eventmaster.catalog.domain.repository.EventRepository;

import java.util.List;

// Strategy — interface que define o contrato de busca de eventos
public interface EventSearchStrategy {
    List<Event> search(EventRepository repository);
}