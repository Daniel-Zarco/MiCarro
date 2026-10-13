package com.micarro.backend.catalog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.micarro.backend.dto.SyncResult;
import com.micarro.backend.entity.Product;
import com.micarro.backend.provider.ProductProvider;
import com.micarro.backend.service.MainCategoryMapper;
import com.micarro.backend.service.VisualGroupMapper;

/*
 * Exporta el catálogo a un JSON estático sin depender de JPA, Flyway ni un
 * datasource. Reutiliza el mismo proveedor (MercadonaProvider) y los mismos
 * mappers que la sincronización a base de datos.
 *
 * El estado previo (para conservar firstSeenAt, detectar cambios de precio y
 * desactivar desaparecidos) se lee del propio catalog/current.json anterior en
 * lugar de PostgreSQL. Si la consulta al proveedor falla o devuelve vacío, el
 * catálogo anterior NO se sobrescribe.
 */
public final class CatalogExporter {

    private static final Logger log = LoggerFactory.getLogger(CatalogExporter.class);

    static final int FORMAT_VERSION = 1;
    static final String CATALOG_FILE_NAME = "catalog.json";

    /*
     * Ventana de retención del historial de precios. Se conservan los puntos de
     * los últimos 60 días, margen suficiente para las novedades de los últimos 30.
     */
    static final long PRICE_HISTORY_RETENTION_DAYS = 60;

    private final ProductProvider productProvider;
    private final MainCategoryMapper mainCategoryMapper;
    private final VisualGroupMapper visualGroupMapper;
    private final Path catalogDirectory;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public CatalogExporter(
            ProductProvider productProvider,
            MainCategoryMapper mainCategoryMapper,
            VisualGroupMapper visualGroupMapper,
            Path catalogDirectory) {

        this(productProvider, mainCategoryMapper, visualGroupMapper,
                catalogDirectory, newObjectMapper(), Clock.systemUTC());
    }

    CatalogExporter(
            ProductProvider productProvider,
            MainCategoryMapper mainCategoryMapper,
            VisualGroupMapper visualGroupMapper,
            Path catalogDirectory,
            ObjectMapper objectMapper,
            Clock clock) {

        this.productProvider = Objects.requireNonNull(productProvider);
        this.mainCategoryMapper = Objects.requireNonNull(mainCategoryMapper);
        this.visualGroupMapper = Objects.requireNonNull(visualGroupMapper);
        this.catalogDirectory = Objects.requireNonNull(catalogDirectory);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.clock = Objects.requireNonNull(clock);
    }

    public Path catalogFile() {
        return catalogDirectory.resolve(CATALOG_FILE_NAME);
    }

    public CatalogExportResult export() {

        Path output = catalogFile();

        CatalogFile previous = readPrevious(output);

        List<Product> incoming;

        try {
            incoming = productProvider.getProducts();
        } catch (RuntimeException exception) {
            String message =
                    "Fallo al consultar la fuente externa; se conserva el catálogo anterior";
            log.error("{}: {}", message, exception.getMessage());
            return CatalogExportResult.failure(message);
        }

        if (incoming == null || incoming.isEmpty()) {
            String message =
                    "La fuente devolvió un catálogo vacío; no se sobrescribe el anterior";
            log.warn(message);
            return CatalogExportResult.failure(message);
        }

        Instant now = clock.instant();
        String source = productProvider.getSource();

        BuildOutcome outcome = build(previous, incoming, source, now);

        writeAtomically(output, outcome.file());

        log.info(
                "Catálogo exportado en {}: recibidos={}, creados={}, actualizados={}, "
                        + "sinCambios={}, desactivados={}, errores={}",
                output,
                outcome.sync().getReceived(),
                outcome.sync().getCreated(),
                outcome.sync().getUpdated(),
                outcome.sync().getUnchanged(),
                outcome.sync().getDeactivated(),
                outcome.sync().getErrors());

        return CatalogExportResult.success(outcome.sync());
    }

    private CatalogFile readPrevious(Path output) {

        if (!Files.exists(output)) {
            return null;
        }

        try {
            return objectMapper.readValue(output.toFile(), CatalogFile.class);
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "No se pudo leer el catálogo previo {} ({}); se regenerará desde cero",
                    output,
                    exception.getMessage());
            return null;
        }
    }

    private BuildOutcome build(
            CatalogFile previous,
            List<Product> incoming,
            String source,
            Instant now) {

        Map<String, CatalogProduct> previousByExternalId = indexPrevious(previous);

        // Deduplicar por externalId; un producto sin externalId es un error
        // puntual que no debe romper el resto de la exportación.
        Map<String, Product> uniqueIncoming = new LinkedHashMap<>();
        int errors = 0;

        for (Product product : incoming) {

            if (product == null) {
                errors++;
                continue;
            }

            String externalId = product.getExternalId();

            if (externalId == null || externalId.isBlank()) {
                errors++;
                continue;
            }

            uniqueIncoming.put(externalId, product);
        }

        int received = incoming.size();
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int deactivated = 0;

        Map<String, CatalogProduct> byExternalId = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();

        int catalogOrder = 0;

        for (Map.Entry<String, Product> entry : uniqueIncoming.entrySet()) {

            String externalId = entry.getKey();
            Product incomingProduct = entry.getValue();

            seen.add(externalId);

            try {

                CatalogProduct previousProduct = previousByExternalId.get(externalId);

                CatalogProduct current = toCatalogProduct(
                        incomingProduct,
                        source,
                        now,
                        catalogOrder,
                        previousProduct
                );

                if (previousProduct == null) {
                    created++;
                } else if (changed(previousProduct, current)) {
                    updated++;
                } else {
                    unchanged++;
                }

                byExternalId.put(externalId, current);

            } catch (RuntimeException exception) {
                errors++;
            }

            catalogOrder++;
        }

        // Los productos que ya no aparecen se conservan marcados como inactivos
        // (nunca se eliminan del JSON), igual que hace la sincronización a BD.
        for (CatalogProduct previousProduct : previousByExternalId.values()) {

            if (previousProduct == null
                    || previousProduct.externalId() == null
                    || seen.contains(previousProduct.externalId())) {
                continue;
            }

            if (previousProduct.active()) {
                byExternalId.put(
                        previousProduct.externalId(),
                        deactivate(previousProduct)
                );
                deactivated++;
            } else {
                byExternalId.put(previousProduct.externalId(), previousProduct);
            }
        }

        List<CatalogProduct> products = new ArrayList<>(byExternalId.values());

        // Activos primero, en el orden del proveedor; después los inactivos.
        products.sort(
                Comparator.comparing(CatalogProduct::active)
                        .reversed()
                        .thenComparingInt(CatalogProduct::catalogOrder)
                        .thenComparing(
                                CatalogProduct::externalId,
                                Comparator.nullsLast(Comparator.naturalOrder())));

        long activeCount = products.stream()
                .filter(CatalogProduct::active)
                .count();

        CatalogFile file = new CatalogFile(
                FORMAT_VERSION,
                source,
                now.toString(),
                products.size(),
                (int) activeCount,
                products);

        return new BuildOutcome(
                file,
                new SyncResult(received, created, updated, unchanged, deactivated, errors));
    }

    private Map<String, CatalogProduct> indexPrevious(CatalogFile previous) {

        Map<String, CatalogProduct> byExternalId = new HashMap<>();

        if (previous == null || previous.products() == null) {
            return byExternalId;
        }

        for (CatalogProduct product : previous.products()) {

            if (product != null && product.externalId() != null) {
                byExternalId.putIfAbsent(product.externalId(), product);
            }
        }

        return byExternalId;
    }

    /*
     * Construye la fila del catálogo. Conserva firstSeenAt del catálogo previo
     * (nunca lo reinicia para un producto ya conocido) y calcula el último
     * cambio de precio detectado: previousPrice es el precio inmediatamente
     * anterior y priceChangedAt el instante en que se detectó. Si no cambia, se
     * arrastra la información del cambio anterior.
     */
    private CatalogProduct toCatalogProduct(
            Product product,
            String source,
            Instant now,
            int catalogOrder,
            CatalogProduct previous) {

        String firstSeenAt = previous == null
                ? now.toString()
                : previous.firstSeenAt();

        BigDecimal price = product.getPrice();

        BigDecimal previousPrice;
        String priceChangedAt;
        List<PricePoint> priceHistory;

        if (previous == null) {
            previousPrice = null;
            priceChangedAt = null;
            priceHistory = price == null
                    ? List.of()
                    : List.of(new PricePoint(price, now.toString()));
        } else if (!samePrice(previous.price(), price)) {
            previousPrice = previous.price();
            priceChangedAt = now.toString();
            priceHistory = trimPriceHistory(
                    appendPricePoint(previous.priceHistory(), new PricePoint(price, now.toString())),
                    now
            );
        } else {
            previousPrice = previous.previousPrice();
            priceChangedAt = previous.priceChangedAt();
            priceHistory = previous.priceHistory() == null
                    ? List.of()
                    : previous.priceHistory();
        }

        return new CatalogProduct(
                product.getExternalId(),
                product.getExternalId(),
                product.getName(),
                product.getBrand(),
                product.getCategory(),
                mainCategoryMapper.map(product.getCategory()),
                visualGroupMapper.visualGroup(product.getCategory()),
                product.getImageUrl(),
                product.getFormat(),
                price,
                previousPrice,
                firstSeenAt,
                priceChangedAt,
                now.toString(),
                catalogOrder,
                true,
                source,
                priceHistory);
    }

    private static CatalogProduct deactivate(CatalogProduct previous) {

        return new CatalogProduct(
                previous.id(),
                previous.externalId(),
                previous.name(),
                previous.brand(),
                previous.category(),
                previous.mainCategory(),
                previous.visualGroup(),
                previous.imageUrl(),
                previous.format(),
                previous.price(),
                previous.previousPrice(),
                previous.firstSeenAt(),
                previous.priceChangedAt(),
                previous.lastSyncedAt(),
                previous.catalogOrder(),
                false,
                previous.source(),
                previous.priceHistory() == null ? List.of() : previous.priceHistory());
    }

    private static List<PricePoint> appendPricePoint(
            List<PricePoint> history,
            PricePoint point) {

        List<PricePoint> appended = new ArrayList<>();

        if (history != null) {
            appended.addAll(history);
        }

        appended.add(point);

        return appended;
    }

    /*
     * Elimina los puntos más antiguos que la ventana de retención para que el
     * JSON no crezca sin límite, conservando lo necesario para las novedades.
     */
    private static List<PricePoint> trimPriceHistory(
            List<PricePoint> history,
            Instant now) {

        Instant cutoff = now.minus(PRICE_HISTORY_RETENTION_DAYS, ChronoUnit.DAYS);

        return history.stream()
                .filter(point -> isAfterOrEqual(point, cutoff))
                .toList();
    }

    private static boolean isAfterOrEqual(PricePoint point, Instant cutoff) {

        if (point == null || point.recordedAt() == null) {
            return false;
        }

        try {
            return !Instant.parse(point.recordedAt()).isBefore(cutoff);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /*
     * Determina si algo sincronizable cambió respecto al catálogo anterior. El
     * estado activo no cuenta como cambio (un producto que reaparece se
     * reactiva pero no se contabiliza como actualizado), igual que en el sync.
     */
    private boolean changed(CatalogProduct previous, CatalogProduct current) {

        return !Objects.equals(previous.name(), current.name())
                || !Objects.equals(previous.category(), current.category())
                || !Objects.equals(previous.mainCategory(), current.mainCategory())
                || !Objects.equals(previous.visualGroup(), current.visualGroup())
                || !Objects.equals(previous.imageUrl(), current.imageUrl())
                || !Objects.equals(previous.format(), current.format())
                || !samePrice(previous.price(), current.price())
                || previous.catalogOrder() != current.catalogOrder();
    }

    private static boolean samePrice(BigDecimal first, BigDecimal second) {

        if (first == null || second == null) {
            return first == second;
        }

        return first.compareTo(second) == 0;
    }

    private void writeAtomically(Path output, CatalogFile file) {

        try {
            Files.createDirectories(catalogDirectory);

            Path temp = Files.createTempFile(catalogDirectory, "current-", ".json.tmp");

            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(temp.toFile(), file);

            move(temp, output);

        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "No se pudo escribir el catálogo en " + output, exception);
        }
    }

    private static void move(Path source, Path target) throws IOException {

        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static ObjectMapper newObjectMapper() {

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }

    private record BuildOutcome(CatalogFile file, SyncResult sync) {
    }
}
