package com.micarro.backend.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.micarro.backend.dto.SyncResult;
import com.micarro.backend.service.ProductSyncService;

/*
 * Sincronización periódica del catálogo.
 * Deshabilitada por defecto (app.catalog-sync.enabled=false); solo se registra
 * cuando se activa explícitamente. No se activa automáticamente en producción.
 */
@Component
@ConditionalOnProperty(
        name = "app.catalog-sync.enabled",
        havingValue = "true"
)
public class CatalogSyncScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(CatalogSyncScheduler.class);

    private final ProductSyncService productSyncService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public CatalogSyncScheduler(ProductSyncService productSyncService) {
        this.productSyncService = productSyncService;
    }

    @Scheduled(cron = "${app.catalog-sync.cron}")
    public void syncCatalog() {

        // Evita ejecuciones solapadas dentro de la misma instancia.
        if (!running.compareAndSet(false, true)) {
            log.warn("Sincronización de catálogo en curso; se omite esta ejecución");
            return;
        }

        try {
            SyncResult result = productSyncService.sync();
            log.info("Catálogo sincronizado: {}", result);
        } catch (Exception exception) {
            log.error("Error sincronizando el catálogo", exception);
        } finally {
            running.set(false);
        }
    }
}
