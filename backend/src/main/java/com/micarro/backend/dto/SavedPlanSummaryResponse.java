package com.micarro.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class SavedPlanSummaryResponse {

    private final Long id;
    private final String name;
    private final Instant createdAt;
    private final int itemCount;
    private final BigDecimal total;

    public SavedPlanSummaryResponse(
            Long id,
            String name,
            Instant createdAt,
            int itemCount,
            BigDecimal total) {

        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.itemCount = itemCount;
        this.total = total;
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

    public int getItemCount() {
        return itemCount;
    }

    public BigDecimal getTotal() {
        return total;
    }
}