package com.eventmaster.catalog.application.usecase;

import com.eventmaster.catalog.application.dto.EventResponse;
import com.eventmaster.catalog.domain.entity.Event;
import com.eventmaster.catalog.domain.repository.EventRepository;

import java.util.List;
import java.util.Optional;

public class SearchEventsUseCase {

    private final EventRepository eventRepository;

    public SearchEventsUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<EventResponse> listAll() {
        return eventRepository.findAll().stream().map(EventResponse::from).toList();
    }

    public Optional<EventResponse> findById(String id) {
        return eventRepository.findById(id).map(EventResponse::from);
    }

    public List<EventResponse> findByCategory(String category) {
        return eventRepository.findByCategory(category).stream().map(EventResponse::from).toList();
    }
}
