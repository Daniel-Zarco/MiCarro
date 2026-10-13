package com.micarro.backend.catalog;

import java.util.List;

/*
 * Contenido completo de catalog/current.json: metadatos de la generación y la
 * lista de productos (activos e inactivos retenidos).
 */
public record CatalogFile(
        int version,
        String source,
        String generatedAt,
        int productCount,
        int activeCount,
        List<CatalogProduct> products) {
}
