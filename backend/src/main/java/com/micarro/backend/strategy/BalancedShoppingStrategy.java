package com.micarro.backend.strategy;

import org.springframework.stereotype.Component;

import com.micarro.backend.model.ShoppingMode;

/**
 * Estrategia equilibrada: la relevancia como factor principal y el precio
 * como secundario.
 */
@Component
public class BalancedShoppingStrategy implements ShoppingStrategy {

    private static final double RELEVANCE_WEIGHT = 0.70;
    private static final double PRICE_WEIGHT = 0.30;

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
}
