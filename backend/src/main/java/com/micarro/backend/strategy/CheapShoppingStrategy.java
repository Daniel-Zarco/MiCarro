package com.micarro.backend.strategy;

import org.springframework.stereotype.Component;

import com.micarro.backend.model.ShoppingMode;

/**
 * Estrategia de ahorro: el precio pesa más que la relevancia.
 */
@Component
public class CheapShoppingStrategy implements ShoppingStrategy {

    private static final double RELEVANCE_WEIGHT = 0.40;
    private static final double PRICE_WEIGHT = 0.60;

    @Override
    public ShoppingMode mode() {
        return ShoppingMode.CHEAP;
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
