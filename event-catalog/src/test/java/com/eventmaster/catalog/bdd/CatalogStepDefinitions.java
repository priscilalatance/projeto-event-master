package com.eventmaster.catalog.bdd;

import com.eventmaster.catalog.application.dto.CreateEventRequest;
import com.eventmaster.catalog.application.dto.EventResponse;
import com.eventmaster.catalog.application.usecase.CreateEventUseCase;
import com.eventmaster.catalog.application.usecase.SearchEventsUseCase;
import com.eventmaster.catalog.domain.entity.Event;
import com.eventmaster.catalog.domain.repository.EventRepository;
import io.cucumber.java.en.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CatalogStepDefinitions {

    private EventRepository repo;
    private EventResponse response;
    private List<EventResponse> responseList;
    private String savedEventId;

    static class InMemoryRepo implements EventRepository {
        private final Map<String, Event> events = new HashMap<>();
        @Override public void save(Event event) { events.put(event.getId(), event); }
        @Override public Optional<Event> findById(String id) { return Optional.ofNullable(events.get(id)); }
        @Override public List<Event> findAll() { return new ArrayList<>(events.values()); }
        @Override public List<Event> findByCategory(String cat) {
            return events.values().stream().filter(e -> e.getCategory().equals(cat)).toList();
        }
    }

    @When("o organizador cadastra o evento {string} com {int} ingressos a {double}")
    public void organizadorCadastraEvento(String name, int tickets, double price) {
        repo = new InMemoryRepo();
        CreateEventUseCase useCase = new CreateEventUseCase(repo);
        response = useCase.execute(new CreateEventRequest(
                name, "Teatro Municipal", LocalDateTime.now().plusDays(30), tickets, price, "TEATRO"));
    }

    @Then("o evento deve ser criado com status {string}")
    public void eventoComStatus(String status) {
        assertEquals(status, response.status());
    }

    @Then("o evento deve ter {int} ingressos disponiveis")
    public void eventoComIngressos(int qty) {
        assertEquals(qty, response.availableTickets());
    }

    @Given("um evento {string} ja cadastrado")
    public void eventoCadastrado(String name) {
        repo = new InMemoryRepo();
        CreateEventUseCase useCase = new CreateEventUseCase(repo);
        response = useCase.execute(new CreateEventRequest(
                name, "Teatro Renault", LocalDateTime.now().plusDays(10), 200, 80.0, "TEATRO"));
        savedEventId = response.id();
    }

    @When("o cliente busca o evento pelo id")
    public void clienteBuscaPorId() {
        SearchEventsUseCase useCase = new SearchEventsUseCase(repo);
        response = useCase.findById(savedEventId).orElse(null);
    }

    @Then("o evento deve ser retornado com nome {string}")
    public void eventoRetornadoComNome(String name) {
        assertNotNull(response);
        assertEquals(name, response.name());
    }

    @Given("eventos cadastrados nas categorias {string} e {string}")
    public void eventosCadastrados(String cat1, String cat2) {
        repo = new InMemoryRepo();
        CreateEventUseCase useCase = new CreateEventUseCase(repo);
        useCase.execute(new CreateEventRequest("Hamlet", "Teatro Municipal", LocalDateTime.now(), 100, 50.0, cat1));
        useCase.execute(new CreateEventRequest("Auto da Compadecida", "Teatro Oficina", LocalDateTime.now(), 100, 50.0, cat1));
        useCase.execute(new CreateEventRequest("O Rei Leao", "Teatro Abril", LocalDateTime.now(), 50, 30.0, cat2));
    }

    @When("o cliente filtra por categoria {string}")
    public void clienteFiltra(String category) {
        SearchEventsUseCase useCase = new SearchEventsUseCase(repo);
        responseList = useCase.findByCategory(category);
    }

    @Then("somente eventos de {string} devem ser retornados")
    public void somenteCategoria(String category) {
        assertFalse(responseList.isEmpty());
        assertTrue(responseList.stream().allMatch(e -> e.category().equals(category)));
    }
}
