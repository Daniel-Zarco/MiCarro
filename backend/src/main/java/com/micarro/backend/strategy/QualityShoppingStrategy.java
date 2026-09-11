package com.micarro.backend.strategy;

import org.springframework.stereotype.Component;

import com.micarro.backend.model.ShoppingMode;

/**
 * Estrategia de calidad: prioriza la relevancia, con un peso residual al
 * precio y un peso moderado para los favoritos.
 *
 * <p>Importante: un precio alto NO se interpreta como mayor calidad. Las
 * métricas reales de calidad (valoraciones, origen, etc.) se incorporarán
 * cuando existan datos fiables.</p>
 */
@Component
public class QualityShoppingStrategy implements ShoppingStrategy {

    private static final double RELEVANCE_WEIGHT = 0.80;
    private static final double PRICE_WEIGHT = 0.05;
    private static final double FAVORITE_WEIGHT = 0.15;

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

    @Override
    public double favoriteWeight() {
        return FAVORITE_WEIGHT;
    }
}
