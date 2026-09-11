package com.micarro.backend.strategy;

import com.micarro.backend.model.ShoppingMode;

/**
 * Define cómo se combinan las puntuaciones de un candidato según el modo
 * de compra: relevancia, precio y favoritos.
 */
public interface ShoppingStrategy {

    ShoppingMode mode();

    double relevanceWeight();

    double priceWeight();

    double favoriteWeight();
}
