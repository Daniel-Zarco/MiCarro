package com.micarro.backend.strategy;

import com.micarro.backend.model.ShoppingMode;

/**
 * Define cómo se combinan las puntuaciones de un candidato según el modo
 * de compra.
 *
 * <p>De momento solo aporta los pesos de relevancia y precio. La puntuación
 * de favoritos todavía no influye (no hay usuarios ni favoritos en backend).</p>
 */
public interface ShoppingStrategy {

    ShoppingMode mode();

    double relevanceWeight();

    double priceWeight();
}
