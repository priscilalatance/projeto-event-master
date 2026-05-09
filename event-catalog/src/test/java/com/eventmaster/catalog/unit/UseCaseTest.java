package com.eventmaster.catalog.unit;

import com.eventmaster.catalog.application.dto.CreateEventRequest;
import com.eventmaster.catalog.application.dto.EventResponse;
import com.eventmaster.catalog.application.usecase.CreateEventUseCase;
import com.eventmaster.catalog.application.usecase.SearchEventsUseCase;
import com.eventmaster.catalog.domain.entity.Event;
import com.eventmaster.catalog.domain.exception.DomainValidationException;
import com.eventmaster.catalog.domain.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class UseCaseTest {

    static class InMemoryEventRepository implements EventRepository {
        private final Map<String, Event> events = new HashMap<>();

        @Override
        public void save(Event event) { events.put(event.getId(), event); }

        @Override
        public Optional<Event> findById(String id) { return Optional.ofNullable(events.get(id)); }

        @Override
        public List<Event> findAll() { return new ArrayList<>(events.values()); }

        @Override
        public List<Event> findByCategory(String category) {
            return events.values().stream()
                    .filter(e -> e.getCategory().equals(category))
                    .toList();
        }
    }

    @Nested
    @DisplayName("CreateEventUseCase")
    class CreateTest {

        @Test
        @DisplayName("deve criar e publicar evento")
        void shouldCreateAndPublish() {
            InMemoryEventRepository repo = new InMemoryEventRepository();
            CreateEventUseCase useCase = new CreateEventUseCase(repo);

            CreateEventRequest request = new CreateEventRequest(
                    "Auto da Compadecida", "Teatro Oficina", LocalDateTime.now().plusDays(10), 500, 80.0, "TEATRO");

            EventResponse response = useCase.execute(request);

            assertNotNull(response.id());
            assertEquals("Auto da Compadecida", response.name());
            assertEquals("PUBLISHED", response.status());
            assertEquals(500, response.availableTickets());
        }

        @Test
        @DisplayName("deve lancar erro com dados invalidos")
        void shouldThrowWithInvalidData() {
            InMemoryEventRepository repo = new InMemoryEventRepository();
            CreateEventUseCase useCase = new CreateEventUseCase(repo);

            CreateEventRequest request = new CreateEventRequest(
                    "", "Teatro", LocalDateTime.now(), 100, 50.0, "TEATRO");

            assertThrows(DomainValidationException.class, () -> useCase.execute(request));
        }
    }

    @Nested
    @DisplayName("SearchEventsUseCase")
    class SearchTest {

        @Test
        @DisplayName("deve listar todos os eventos")
        void shouldListAll() {
            InMemoryEventRepository repo = new InMemoryEventRepository();
            Event e1 = new Event("Hamlet", "Teatro Municipal", LocalDateTime.now(), 100, 50.0, "TEATRO");
            Event e2 = new Event("O Rei Leao", "Teatro Abril", LocalDateTime.now(), 50, 30.0, "MUSICAL");
            repo.save(e1);
            repo.save(e2);

            SearchEventsUseCase useCase = new SearchEventsUseCase(repo);
            List<EventResponse> result = useCase.listAll();

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("deve retornar lista vazia")
        void shouldReturnEmpty() {
            InMemoryEventRepository repo = new InMemoryEventRepository();
            SearchEventsUseCase useCase = new SearchEventsUseCase(repo);

            assertTrue(useCase.listAll().isEmpty());
        }

        @Test
        @DisplayName("deve buscar por id")
        void shouldFindById() {
            InMemoryEventRepository repo = new InMemoryEventRepository();
            Event event = new Event("Morte e Vida Severina", "SESC Pinheiros", LocalDateTime.now(), 200, 120.0, "TEATRO");
            repo.save(event);

            SearchEventsUseCase useCase = new SearchEventsUseCase(repo);
            Optional<EventResponse> result = useCase.findById(event.getId());

            assertTrue(result.isPresent());
            assertEquals("Morte e Vida Severina", result.get().name());
        }

        @Test
        @DisplayName("deve retornar vazio para id inexistente")
        void shouldReturnEmptyForInvalidId() {
            InMemoryEventRepository repo = new InMemoryEventRepository();
            SearchEventsUseCase useCase = new SearchEventsUseCase(repo);

            assertTrue(useCase.findById("inexistente").isEmpty());
        }

        @Test
        @DisplayName("deve filtrar por categoria")
        void shouldFilterByCategory() {
            InMemoryEventRepository repo = new InMemoryEventRepository();
            repo.save(new Event("Hamlet", "Teatro Municipal", LocalDateTime.now(), 100, 50.0, "TEATRO"));
            repo.save(new Event("O Rei Leao", "Teatro Abril", LocalDateTime.now(), 50, 30.0, "MUSICAL"));
            repo.save(new Event("Auto da Compadecida", "Teatro Oficina", LocalDateTime.now(), 30, 20.0, "TEATRO"));

            SearchEventsUseCase useCase = new SearchEventsUseCase(repo);
            List<EventResponse> result = useCase.findByCategory("TEATRO");

            assertEquals(2, result.size());
        }
    }
}
