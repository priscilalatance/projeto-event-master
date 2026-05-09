package com.eventmaster.catalog.integration;

import com.eventmaster.catalog.application.dto.CreateEventRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Integracao - EventController")
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /events/health deve retornar status ok")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/events/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("event-catalog"));
    }

    @Test
    @DisplayName("GET /events deve retornar lista de eventos")
    void listAll() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /events deve criar evento com sucesso")
    void createEvent() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "Hamlet", "Teatro Municipal", LocalDateTime.now().plusDays(30),
                500, 120.0, "TEATRO");

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Hamlet"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.availableTickets").value(500));
    }

    @Test
    @DisplayName("POST /events com nome vazio deve retornar 400")
    void createEventInvalid() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "", "Teatro", LocalDateTime.now(), 100, 50.0, "TEATRO");

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Nome do evento e obrigatorio"));
    }

    @Test
    @DisplayName("GET /events/{id} inexistente deve retornar 404")
    void findByIdNotFound() throws Exception {
        mockMvc.perform(get("/events/id-inexistente"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /events?category deve filtrar por categoria")
    void filterByCategory() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "Auto da Compadecida", "Teatro Oficina", LocalDateTime.now().plusDays(10),
                300, 80.0, "TEATRO");

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/events").param("category", "TEATRO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("TEATRO"));
    }
}
