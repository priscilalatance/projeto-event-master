package com.eventmaster.catalog.application.strategy;

import com.eventmaster.catalog.domain.entity.Event;
import com.eventmaster.catalog.domain.repository.EventRepository;
import com.eventmaster.catalog.domain.strategy.EventSearchStrategy;

import java.util.List;

public class CategoryFilterStrategy implements EventSearchStrategy {

    private final String category;

    public CategoryFilterStrategy(String category) {
        this.category = category;
    }

    @Override
    public List<Event> search(EventRepository repository) {
        return repository.findByCategory(category);
    }
}