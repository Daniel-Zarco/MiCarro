package com.micarro.backend.strategy;

import org.springframework.stereotype.Component;

import com.micarro.backend.model.ShoppingMode;

/**
 * Estrategia de calidad: prioriza la relevancia.
 *
 * <p>Importante: un precio alto NO se interpreta como mayor calidad. Por ahora
 * esta estrategia solo da más peso a la relevancia y un peso residual al
 * precio. Las métricas reales de calidad (valoraciones, origen, etc.) se
 * incorporarán cuando existan datos fiables.</p>
 */
@Component
public class QualityShoppingStrategy implements ShoppingStrategy {

    private static final double RELEVANCE_WEIGHT = 0.90;
    private static final double PRICE_WEIGHT = 0.10;

    @Override
    public ShoppingMode mode() {
        return ShoppingMode.QUALITY;
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
