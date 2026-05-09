package com.eventmaster.catalog.infrastructure.repository;

import com.eventmaster.catalog.domain.entity.Event;
import com.eventmaster.catalog.domain.repository.EventRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaEventRepositoryAdapter implements EventRepository {

    private final JpaEventRepository jpaRepository;

    public JpaEventRepositoryAdapter(JpaEventRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Event event) {
        EventEntity entity = EventEntity.builder()
                .id(event.getId())
                .name(event.getName())
                .venue(event.getVenue())
                .date(event.getDate())
                .totalTickets(event.getTotalTickets())
                .availableTickets(event.getAvailableTickets())
                .price(event.getPrice())
                .category(event.getCategory())
                .status(event.getStatus())
                .build();
        jpaRepository.save(entity);
    }

    @Override
    public Optional<Event> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Event> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Event> findByCategory(String category) {
        return jpaRepository.findByCategory(category).stream().map(this::toDomain).toList();
    }

    private Event toDomain(EventEntity e) {
        return new Event(e.getId(), e.getName(), e.getVenue(), e.getDate(),
                e.getTotalTickets(), e.getAvailableTickets(),
                e.getPrice(), e.getCategory(), e.getStatus());
    }
}
