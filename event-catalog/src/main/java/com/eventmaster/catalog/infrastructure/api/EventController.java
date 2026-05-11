package com.eventmaster.catalog.infrastructure.api;

import com.eventmaster.catalog.application.dto.CreateEventRequest;
import com.eventmaster.catalog.application.dto.EventResponse;
import com.eventmaster.catalog.application.strategy.AllEventsStrategy;
import com.eventmaster.catalog.application.strategy.CategoryFilterStrategy;
import com.eventmaster.catalog.application.usecase.CreateEventUseCase;
import com.eventmaster.catalog.application.usecase.SearchEventsUseCase;
import com.eventmaster.catalog.domain.exception.EventNotFoundException;
import com.eventmaster.catalog.domain.strategy.EventSearchStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/events")
public class EventController {

    private final CreateEventUseCase createEventUseCase;
    private final SearchEventsUseCase searchEventsUseCase;

    public EventController(CreateEventUseCase createEventUseCase, SearchEventsUseCase searchEventsUseCase) {
        this.createEventUseCase = createEventUseCase;
        this.searchEventsUseCase = searchEventsUseCase;
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createEventUseCase.execute(request));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> listAll(@RequestParam(required = false) String category) {
        EventSearchStrategy strategy = (category != null)
                ? new CategoryFilterStrategy(category)
                : new AllEventsStrategy();
        return ResponseEntity.ok(searchEventsUseCase.execute(strategy));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> findById(@PathVariable String id) {
        return searchEventsUseCase.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "event-catalog"));
    }
}