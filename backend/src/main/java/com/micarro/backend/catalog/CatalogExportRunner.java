package com.micarro.backend.catalog;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.micarro.backend.external.mercadona.MercadonaClient;
import com.micarro.backend.provider.MercadonaProvider;
import com.micarro.backend.service.MainCategoryMapper;
import com.micarro.backend.service.VisualGroupMapper;

/*
 * Punto de entrada del exportador de catálogo. Se ejecuta como programa Java
 * normal (sin Spring, sin JPA, sin datasource, sin Flyway), por lo que puede
 * correr en local sin PostgreSQL.
 *
 * Directorio de salida (en orden de prioridad):
 *   1. Primer argumento de la línea de comandos.
 *   2. Propiedad de sistema -Dmicarro.catalog.dir.
 *   3. Variable de entorno MICARRO_CATALOG_DIR.
 *   4. "catalog" relativo al directorio actual.
 *
 * URL de la fuente (en orden de prioridad):
 *   1. Propiedad de sistema -Dmicarro.mercadona.base-url.
 *   2. Variable de entorno MERCADONA_BASE_URL.
 *   3. Valor por defecto.
 */
public final class CatalogExportRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogExportRunner.class);

    private static final String DEFAULT_BASE_URL = "https://tienda.mercadona.es/api";

    private CatalogExportRunner() {
    }

    public static void main(String[] args) {

        Path catalogDirectory = resolveCatalogDirectory(args);
        String baseUrl = resolveBaseUrl();

        log.info("Generando catálogo JSON en {} (fuente: {})", catalogDirectory, baseUrl);

        CatalogExporter exporter = new CatalogExporter(
                new MercadonaProvider(new MercadonaClient(baseUrl)),
                new MainCategoryMapper(),
                new VisualGroupMapper(),
                catalogDirectory);

        CatalogExportResult result = exporter.export();

        if (result.success()) {
            log.info("Catálogo generado correctamente: {}", result.sync());
            return;
        }

        // Se lanza excepción para que la ejecución (Maven, CI, script) termine
        // con código de error sin haber sobrescrito el catálogo anterior.
        throw new IllegalStateException(
                "No se pudo generar el catálogo: " + result.message());
    }

    private static Path resolveCatalogDirectory(String[] args) {

        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            return Paths.get(args[0]);
        }

        String property = System.getProperty("micarro.catalog.dir");

        if (property != null && !property.isBlank()) {
            return Paths.get(property);
        }

        String environment = System.getenv("MICARRO_CATALOG_DIR");

        if (environment != null && !environment.isBlank()) {
            return Paths.get(environment);
        }

        return Paths.get("catalog");
    }

    private static String resolveBaseUrl() {

        String property = System.getProperty("micarro.mercadona.base-url");

        if (property != null && !property.isBlank()) {
            return property;
        }

        String environment = System.getenv("MERCADONA_BASE_URL");

        if (environment != null && !environment.isBlank()) {
            return environment;
        }

        return DEFAULT_BASE_URL;
    }
}
