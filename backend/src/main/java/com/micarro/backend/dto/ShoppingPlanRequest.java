package com.micarro.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.micarro.backend.model.ShoppingMode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ShoppingPlanRequest {

    @NotNull(message = "El presupuesto es obligatorio")
    @Positive(message = "El presupuesto debe ser mayor que 0")
    private BigDecimal budget;

    @NotEmpty(message = "Debes indicar al menos un producto")
    private List<String> items;

    @NotNull(message = "El modo de compra es obligatorio")
    private ShoppingMode mode;

    @Valid
    private ShoppingPreferences preferences;

    public ShoppingPlanRequest() {
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public ShoppingMode getMode() {
        return mode;
    }

    public void setMode(ShoppingMode mode) {
        this.mode = mode;
    }

    public ShoppingPreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(ShoppingPreferences preferences) {
        this.preferences = preferences;
    }
}
