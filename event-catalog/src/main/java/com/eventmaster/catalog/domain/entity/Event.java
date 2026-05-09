package com.eventmaster.catalog.domain.entity;

import com.eventmaster.catalog.domain.exception.BusinessRuleException;
import com.eventmaster.catalog.domain.exception.DomainValidationException;

import java.time.LocalDateTime;
import java.util.UUID;

public class Event {

    private String id;
    private String name;
    private String venue;
    private LocalDateTime date;
    private int totalTickets;
    private int availableTickets;
    private double price;
    private String category;
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

    public boolean hasAvailability() {
        return availableTickets > 0 && status == EventStatus.PUBLISHED;
    }

    public void decrementStock() {
        if (!hasAvailability()) {
            throw new BusinessRuleException("Sem ingressos disponiveis");
        }
        availableTickets--;
    }

    public void incrementStock() {
        if (availableTickets >= totalTickets) {
            throw new BusinessRuleException("Estoque ja esta completo");
        }
        availableTickets++;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getVenue() { return venue; }
    public LocalDateTime getDate() { return date; }
    public int getTotalTickets() { return totalTickets; }
    public int getAvailableTickets() { return availableTickets; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }
    public EventStatus getStatus() { return status; }
}
