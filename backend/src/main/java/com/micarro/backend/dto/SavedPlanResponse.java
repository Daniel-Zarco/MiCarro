package com.micarro.backend.dto;

import java.time.Instant;
import java.util.List;

public class SavedPlanResponse {

    private final Long id;
    private final String name;
    private final Instant createdAt;
    private final List<SavedPlanItemResponse> items;

    public SavedPlanResponse(
            Long id,
            String name,
            Instant createdAt,
            List<SavedPlanItemResponse> items) {

        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.items = items;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<SavedPlanItemResponse> getItems() {
        return items;
    }
}