package com.micarro.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.micarro.backend.model.ShoppingMode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class ShoppingPlanHistoryRequest {

    @NotNull(message = "El presupuesto es obligatorio")
    @Positive(message = "El presupuesto debe ser mayor que 0")
    private BigDecimal budget;

    @NotNull(message = "El total estimado es obligatorio")
    @PositiveOrZero(message = "El total estimado no puede ser negativo")
    private BigDecimal estimatedTotal;

    @NotNull(message = "El modo de compra es obligatorio")
    private ShoppingMode mode;

    @NotNull(message = "Los items son obligatorios")
    private List<@Valid ShoppingPlanHistoryItemRequest> items;

    public ShoppingPlanHistoryRequest() {
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public BigDecimal getEstimatedTotal() {
        return estimatedTotal;
    }

    public void setEstimatedTotal(BigDecimal estimatedTotal) {
        this.estimatedTotal = estimatedTotal;
    }

    public ShoppingMode getMode() {
        return mode;
    }

    public void setMode(ShoppingMode mode) {
        this.mode = mode;
    }

    public List<ShoppingPlanHistoryItemRequest> getItems() {
        return items;
    }

    public void setItems(List<ShoppingPlanHistoryItemRequest> items) {
        this.items = items;
    }
}
