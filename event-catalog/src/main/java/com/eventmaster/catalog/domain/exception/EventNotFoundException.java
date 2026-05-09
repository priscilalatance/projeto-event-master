package com.eventmaster.catalog.domain.exception;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(String id) {
        super("Evento nao encontrado: " + id);
    }
}
