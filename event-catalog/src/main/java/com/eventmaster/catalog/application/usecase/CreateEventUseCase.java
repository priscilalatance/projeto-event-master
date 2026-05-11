package com.eventmaster.catalog.application.usecase;

import com.eventmaster.catalog.application.dto.CreateEventRequest;
import com.eventmaster.catalog.application.dto.EventResponse;
import com.eventmaster.catalog.domain.entity.Event;
import com.eventmaster.catalog.domain.repository.EventRepository;
import org.springframework.stereotype.Service;


@Service
public class CreateEventUseCase {

    private final EventRepository eventRepository;

    public CreateEventUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public EventResponse execute(CreateEventRequest request) {
        Event event = new Event(
                request.name(), request.venue(), request.date(),
                request.totalTickets(), request.price(), request.category()
        );
        event.publish();
        eventRepository.save(event);
        return EventResponse.from(event);
    }
}
