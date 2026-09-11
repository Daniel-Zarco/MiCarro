package com.micarro.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.micarro.backend.model.ShoppingMode;

public class ShoppingPlanResponse {

    private BigDecimal budget;
    private ShoppingMode mode;
    private ShoppingPreferences preferences;
    private List<ShoppingPlanItemResponse> items;
    private BigDecimal estimatedTotal;
    private BigDecimal remainingBudget;

    public ShoppingPlanResponse() {
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
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

    public List<ShoppingPlanItemResponse> getItems() {
        return items;
    }

    public void setItems(List<ShoppingPlanItemResponse> items) {
        this.items = items;
    }

    public BigDecimal getEstimatedTotal() {
        return estimatedTotal;
    }

    public void setEstimatedTotal(BigDecimal estimatedTotal) {
        this.estimatedTotal = estimatedTotal;
    }

    public BigDecimal getRemainingBudget() {
        return remainingBudget;
    }

    public void setRemainingBudget(BigDecimal remainingBudget) {
        this.remainingBudget = remainingBudget;
    }
}
