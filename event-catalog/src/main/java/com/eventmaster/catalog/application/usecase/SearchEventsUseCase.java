package com.eventmaster.catalog.application.usecase;

import com.eventmaster.catalog.application.dto.EventResponse;
import com.eventmaster.catalog.domain.repository.EventRepository;
import com.eventmaster.catalog.domain.strategy.EventSearchStrategy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SearchEventsUseCase {

    private final EventRepository eventRepository;

    public SearchEventsUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<EventResponse> listAll() {
        return eventRepository.findAll().stream().map(EventResponse::from).toList();
    }

    public List<EventResponse> execute(EventSearchStrategy strategy) {
        return strategy.search(eventRepository).stream()
                .map(EventResponse::from)
                .toList();
    }

    public Optional<EventResponse> findById(String id) {
        return eventRepository.findById(id).map(EventResponse::from);
    }
}
