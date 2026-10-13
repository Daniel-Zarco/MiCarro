package com.micarro.backend.catalog;

import com.micarro.backend.dto.SyncResult;

/*
 * Resultado de una exportación. `success=false` significa que no se escribió
 * nada y, si existía, el catálogo anterior permanece intacto.
 */
public record CatalogExportResult(
        boolean success,
        String message,
        SyncResult sync) {

    static CatalogExportResult success(SyncResult sync) {
        return new CatalogExportResult(true, "ok", sync);
    }

    static CatalogExportResult failure(String message) {
        return new CatalogExportResult(false, message, null);
    }
}
