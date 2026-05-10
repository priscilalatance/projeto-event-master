package com.eventmaster.catalog.unit;

import com.eventmaster.catalog.application.dto.CreateEventRequest;
import com.eventmaster.catalog.application.dto.EventResponse;
import com.eventmaster.catalog.application.strategy.AllEventsStrategy;
import com.eventmaster.catalog.application.strategy.CategoryFilterStrategy;
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
    @DisplayName("SearchEventsUseCase — Strategy Pattern")
    class SearchTest {

        @Test
        @DisplayName("AllEventsStrategy deve retornar todos os eventos")
        void shouldListAllWithStrategy() {
            InMemoryEventRepository repo = new InMemoryEventRepository();
            repo.save(new Event("Hamlet", "Teatro Municipal", LocalDateTime.now(), 100, 50.0, "TEATRO"));
            repo.save(new Event("O Rei Leao", "Teatro Abril", LocalDateTime.now(), 50, 30.0, "MUSICAL"));

            SearchEventsUseCase useCase = new SearchEventsUseCase(repo);
            List<EventResponse> result = useCase.execute(new AllEventsStrategy());

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("AllEventsStrategy deve retornar lista vazia quando repositorio vazio")
        void shouldReturnEmptyWithAllStrategy() {
            SearchEventsUseCase useCase = new SearchEventsUseCase(new InMemoryEventRepository());
            assertTrue(useCase.execute(new AllEventsStrategy()).isEmpty());
        }

        @Test
        @DisplayName("CategoryFilterStrategy deve filtrar por categoria")
        void shouldFilterByCategoryWithStrategy() {
            InMemoryEventRepository repo = new InMemoryEventRepository();
            repo.save(new Event("Hamlet", "Teatro Municipal", LocalDateTime.now(), 100, 50.0, "TEATRO"));
            repo.save(new Event("O Rei Leao", "Teatro Abril", LocalDateTime.now(), 50, 30.0, "MUSICAL"));
            repo.save(new Event("Auto da Compadecida", "Teatro Oficina", LocalDateTime.now(), 30, 20.0, "TEATRO"));

            SearchEventsUseCase useCase = new SearchEventsUseCase(repo);
            List<EventResponse> result = useCase.execute(new CategoryFilterStrategy("TEATRO"));

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(e -> e.category().equals("TEATRO")));
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
            SearchEventsUseCase useCase = new SearchEventsUseCase(new InMemoryEventRepository());
            assertTrue(useCase.findById("inexistente").isEmpty());
        }
    }
}