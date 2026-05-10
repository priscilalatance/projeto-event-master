package com.eventmaster.catalog.unit;

import com.eventmaster.catalog.domain.entity.Event;
import com.eventmaster.catalog.domain.entity.EventStatus;
import com.eventmaster.catalog.domain.exception.BusinessRuleException;
import com.eventmaster.catalog.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    private Event criarEvento(int totalTickets) {
        return new Event("Hamlet", "Teatro Municipal SP", LocalDateTime.now().plusDays(30), totalTickets, 150.0, "TEATRO");
    }

    @Nested
    @DisplayName("Criacao de Evento")
    class CriacaoTest {

        @Test
        @DisplayName("deve criar evento com dados validos")
        void shouldCreateEvent() {
            Event event = criarEvento(100);
            assertNotNull(event.getId());
            assertEquals("Hamlet", event.getName());
            assertEquals(100, event.getAvailableTickets());
            assertEquals(EventStatus.DRAFT, event.getStatus());
        }

        @Test
        @DisplayName("deve lancar erro com nome vazio")
        void shouldThrowWhenNameBlank() {
            assertThrows(DomainValidationException.class, () ->
                    new Event("", "Teatro", LocalDateTime.now(), 100, 50.0, "TEATRO"));
        }

        @Test
        @DisplayName("deve lancar erro com nome nulo")
        void shouldThrowWhenNameNull() {
            assertThrows(DomainValidationException.class, () ->
                    new Event(null, "Teatro", LocalDateTime.now(), 100, 50.0, "TEATRO"));
        }

        @Test
        @DisplayName("deve lancar erro com total de ingressos zero")
        void shouldThrowWhenZeroTickets() {
            assertThrows(DomainValidationException.class, () ->
                    new Event("Hamlet", "Teatro", LocalDateTime.now(), 0, 50.0, "TEATRO"));
        }

        @Test
        @DisplayName("deve lancar erro com preco negativo")
        void shouldThrowWhenNegativePrice() {
            assertThrows(DomainValidationException.class, () ->
                    new Event("Hamlet", "Teatro", LocalDateTime.now(), 100, -10.0, "TEATRO"));
        }
    }

    @Nested
    @DisplayName("Ciclo de Vida do Evento")
    class CicloDeVidaTest {

        @Test
        @DisplayName("deve publicar evento em DRAFT")
        void shouldPublish() {
            Event event = criarEvento(100);
            event.publish();
            assertEquals(EventStatus.PUBLISHED, event.getStatus());
        }

        @Test
        @DisplayName("deve lancar erro ao publicar evento ja publicado")
        void shouldThrowWhenPublishingPublished() {
            Event event = criarEvento(100);
            event.publish();
            assertThrows(BusinessRuleException.class, event::publish);
        }

        @Test
        @DisplayName("deve cancelar evento")
        void shouldCancel() {
            Event event = criarEvento(100);
            event.cancel();
            assertEquals(EventStatus.CANCELLED, event.getStatus());
        }

        @Test
        @DisplayName("nao deve cancelar evento finalizado")
        void shouldThrowWhenCancellingFinished() {
            Event event = criarEvento(100);
            event.publish();
            event.finish();
            assertThrows(BusinessRuleException.class, event::cancel);
        }

        @Test
        @DisplayName("deve finalizar evento publicado")
        void shouldFinish() {
            Event event = criarEvento(100);
            event.publish();
            event.finish();
            assertEquals(EventStatus.FINISHED, event.getStatus());
        }

        @Test
        @DisplayName("nao deve finalizar evento em DRAFT")
        void shouldThrowWhenFinishingDraft() {
            Event event = criarEvento(100);
            assertThrows(BusinessRuleException.class, event::finish);
        }

        @Test
        @DisplayName("deve ter disponibilidade quando publicado e com estoque")
        void shouldHaveAvailability() {
            Event event = criarEvento(5);
            event.publish();
            assertTrue(event.hasAvailability());
        }

        @Test
        @DisplayName("nao deve ter disponibilidade quando em DRAFT")
        void shouldNotHaveAvailabilityWhenDraft() {
            Event event = criarEvento(5);
            assertFalse(event.hasAvailability());
        }
    }
}