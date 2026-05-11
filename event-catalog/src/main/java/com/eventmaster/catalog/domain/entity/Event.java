package com.eventmaster.catalog.domain.entity;

import com.eventmaster.catalog.domain.exception.BusinessRuleException;
import com.eventmaster.catalog.domain.exception.DomainValidationException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Event {

    private final String id;
    private final String name;
    private final String venue;
    private final LocalDateTime date;
    private final int totalTickets;
    private final int availableTickets;
    private final double price;
    private final String category;
    private EventStatus status;

    public Event(String name, String venue, LocalDateTime date, int totalTickets, double price, String category) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Nome do evento e obrigatorio");
        }
        if (totalTickets <= 0) {
            throw new DomainValidationException("Total de ingressos deve ser maior que zero");
        }
        if (price < 0) {
            throw new DomainValidationException("Preco nao pode ser negativo");
        }
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.venue = venue;
        this.date = date;
        this.totalTickets = totalTickets;
        this.availableTickets = totalTickets;
        this.price = price;
        this.category = category;
        this.status = EventStatus.DRAFT;
    }

    // Construtor de reconstrução a partir da persistência
    public Event(String id, String name, String venue, LocalDateTime date,
                 int totalTickets, int availableTickets, double price, String category, EventStatus status) {
        this.id = id;
        this.name = name;
        this.venue = venue;
        this.date = date;
        this.totalTickets = totalTickets;
        this.availableTickets = availableTickets;
        this.price = price;
        this.category = category;
        this.status = status;
    }

    public void publish() {
        if (this.status != EventStatus.DRAFT) {
            throw new BusinessRuleException("Somente eventos em DRAFT podem ser publicados");
        }
        this.status = EventStatus.PUBLISHED;
    }

    public void cancel() {
        if (this.status == EventStatus.FINISHED) {
            throw new BusinessRuleException("Evento finalizado nao pode ser cancelado");
        }
        this.status = EventStatus.CANCELLED;
    }

    public void finish() {
        if (this.status != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("Somente eventos PUBLISHED podem ser finalizados");
        }
        this.status = EventStatus.FINISHED;
    }

    public boolean hasAvailability() {
        return availableTickets > 0 && status == EventStatus.PUBLISHED;
    }
}