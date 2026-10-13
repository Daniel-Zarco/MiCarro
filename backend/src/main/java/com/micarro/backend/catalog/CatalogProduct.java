package com.micarro.backend.catalog;

import java.math.BigDecimal;
import java.util.List;

/*
 * Producto tal y como se publica en catalog.json.
 *
 * `id` y `externalId` comparten el externalId del proveedor: es el identificador
 * estable que el frontend usará como clave (carrito, favoritos, planes...).
 * Las fechas se serializan como texto ISO-8601 para que el JSON no dependa de
 * módulos de Jackson para tipos temporales.
 */
public record CatalogProduct(
        String id,
        String externalId,
        String name,
        String brand,
        String category,
        String mainCategory,
        String visualGroup,
        String imageUrl,
        String format,
        BigDecimal price,
        BigDecimal previousPrice,
        String firstSeenAt,
        String priceChangedAt,
        String lastSyncedAt,
        int catalogOrder,
        boolean active,
        String source,
        List<PricePoint> priceHistory) {
}
