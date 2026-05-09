package com.eventmaster.catalog.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JpaEventRepository extends JpaRepository<EventEntity, String> {
    List<EventEntity> findByCategory(String category);
}
