package com.eventmaster.catalog.infrastructure.api;

import com.eventmaster.catalog.application.dto.CreateEventRequest;
import com.eventmaster.catalog.application.dto.EventResponse;
import com.eventmaster.catalog.application.usecase.CreateEventUseCase;
import com.eventmaster.catalog.application.usecase.SearchEventsUseCase;
import com.eventmaster.catalog.domain.repository.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventRepository eventRepository;

    public EventController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@RequestBody CreateEventRequest request) {
        CreateEventUseCase useCase = new CreateEventUseCase(eventRepository);
        EventResponse response = useCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> listAll(@RequestParam(required = false) String category) {
        SearchEventsUseCase useCase = new SearchEventsUseCase(eventRepository);
        List<EventResponse> events = (category != null)
                ? useCase.findByCategory(category)
                : useCase.listAll();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> findById(@PathVariable String id) {
        SearchEventsUseCase useCase = new SearchEventsUseCase(eventRepository);
        return useCase.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "event-catalog"));
    }
}
