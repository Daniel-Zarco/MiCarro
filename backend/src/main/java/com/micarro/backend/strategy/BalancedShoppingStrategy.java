package com.micarro.backend.strategy;

import org.springframework.stereotype.Component;

import com.micarro.backend.model.ShoppingMode;

/**
 * Estrategia equilibrada: la relevancia como factor principal, el precio como
 * secundario y un peso moderado para los favoritos.
 */
@Component
public class BalancedShoppingStrategy implements ShoppingStrategy {

    private static final double RELEVANCE_WEIGHT = 0.60;
    private static final double PRICE_WEIGHT = 0.25;
    private static final double FAVORITE_WEIGHT = 0.15;

    @Override
    public ShoppingMode mode() {
        return ShoppingMode.BALANCED;
    }

    @Override
    public double relevanceWeight() {
        return RELEVANCE_WEIGHT;
    }

    @Override
    public double priceWeight() {
        return PRICE_WEIGHT;
    }

    @Override
    public double favoriteWeight() {
        return FAVORITE_WEIGHT;
    }
}
