package com.eventmaster.catalog.infrastructure.repository;

import com.eventmaster.catalog.domain.entity.EventStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String venue;

    @Column(name = "event_date")
    private LocalDateTime date;

    @Column(name = "total_tickets")
    private int totalTickets;

    @Column(name = "available_tickets")
    private int availableTickets;

    private double price;
    private String category;

    @Enumerated(EnumType.STRING)
    private EventStatus status;
}
