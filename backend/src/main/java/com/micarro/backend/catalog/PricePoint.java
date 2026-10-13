package com.micarro.backend.catalog;

import java.math.BigDecimal;

/*
 * Punto de la serie de precios de un producto en catalog.json. Junto con
 * CatalogProduct.priceHistory permite reconstruir el precio vigente en una
 * fecha concreta (p. ej. hace 30 días) para las novedades de precio.
 */
public record PricePoint(BigDecimal price, String recordedAt) {
}
