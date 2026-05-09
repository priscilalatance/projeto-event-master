package com.eventmaster.catalog.application.dto;

import java.time.LocalDateTime;

public record CreateEventRequest(
        String name,
        String venue,
        LocalDateTime date,
        int totalTickets,
        double price,
        String category
) {}
