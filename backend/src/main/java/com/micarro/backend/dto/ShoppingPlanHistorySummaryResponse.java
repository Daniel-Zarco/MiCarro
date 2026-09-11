package com.micarro.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.micarro.backend.model.ShoppingMode;

public class ShoppingPlanHistorySummaryResponse {

    private final Long id;
    private final BigDecimal budget;
    private final BigDecimal estimatedTotal;
    private final ShoppingMode mode;
    private final Instant createdAt;

    public ShoppingPlanHistorySummaryResponse(
            Long id,
            BigDecimal budget,
            BigDecimal estimatedTotal,
            ShoppingMode mode,
            Instant createdAt) {

        this.id = id;
        this.budget = budget;
        this.estimatedTotal = estimatedTotal;
        this.mode = mode;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public BigDecimal getEstimatedTotal() {
        return estimatedTotal;
    }

    public ShoppingMode getMode() {
        return mode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
