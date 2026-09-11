package com.micarro.backend.dto;

public class ShoppingPreferences {

    private boolean prioritizeFavorites;
    private boolean maximizeBudget;

    public ShoppingPreferences() {
    }

    public boolean isPrioritizeFavorites() {
        return prioritizeFavorites;
    }

    public void setPrioritizeFavorites(boolean prioritizeFavorites) {
        this.prioritizeFavorites = prioritizeFavorites;
    }

    public boolean isMaximizeBudget() {
        return maximizeBudget;
    }

    public void setMaximizeBudget(boolean maximizeBudget) {
        this.maximizeBudget = maximizeBudget;
    }
}
