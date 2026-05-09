package com.eventmaster.catalog.application.dto;

import com.eventmaster.catalog.domain.entity.Event;
import java.time.LocalDateTime;

public record EventResponse(
        String id,
        String name,
        String venue,
        LocalDateTime date,
        int totalTickets,
        int availableTickets,
        double price,
        String category,
        String status
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(), event.getName(), event.getVenue(), event.getDate(),
                event.getTotalTickets(), event.getAvailableTickets(),
                event.getPrice(), event.getCategory(), event.getStatus().name()
        );
    }
}
