package com.micarro.backend.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.micarro.backend.entity.Product;
import com.micarro.backend.provider.ProductProvider;
import com.micarro.backend.service.MainCategoryMapper;
import com.micarro.backend.service.VisualGroupMapper;

class CatalogExporterTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @TempDir
    Path tempDir;

    private CatalogExporter exporter(ProductProvider provider, Clock clock) {
        return new CatalogExporter(
                provider,
                new MainCategoryMapper(),
                new VisualGroupMapper(),
                tempDir,
                MAPPER,
                clock);
    }

    private CatalogExporter exporter(ProductProvider provider) {
        return exporter(provider, Clock.systemUTC());
    }

    private CatalogFile readCatalog() throws IOException {
        return MAPPER.readValue(
                tempDir.resolve(CatalogExporter.CATALOG_FILE_NAME).toFile(),
                CatalogFile.class);
    }

    private static CatalogProduct byId(CatalogFile catalog, String externalId) {
        return catalog.products().stream()
                .filter(product -> externalId.equals(product.externalId()))
                .findFirst()
                .orElseThrow();
    }

    private static Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    private static Product product(
            String externalId,
            String name,
            String category,
            String imageUrl,
            String format,
            String price) {

        Product product = new Product();
        product.setExternalId(externalId);
        product.setName(name);
        product.setCategory(category);
        product.setImageUrl(imageUrl);
        product.setFormat(format);
        product.setPrice(price == null ? null : new BigDecimal(price));

        return product;
    }

    @Test
    void export_generatesCatalogWithFrontendFields() throws Exception {

        StubProvider provider = new StubProvider("MERCADONA");
        provider.returnProducts(List.of(
                product("A", "Pollo entero", "Pollo", "http://img/a.jpg", "1 kg", "5.20"),
                product("B", "Manzanas", "Manzana y pera", "http://img/b.jpg", "1 kg", "1.99")));

        CatalogExportResult result = exporter(provider).export();

        assertThat(result.success()).isTrue();
        assertThat(result.sync().getReceived()).isEqualTo(2);
        assertThat(result.sync().getCreated()).isEqualTo(2);
        assertThat(tempDir.resolve(CatalogExporter.CATALOG_FILE_NAME)).exists();

        CatalogFile catalog = readCatalog();

        assertThat(catalog.version()).isEqualTo(CatalogExporter.FORMAT_VERSION);
        assertThat(catalog.source()).isEqualTo("MERCADONA");
        assertThat(catalog.productCount()).isEqualTo(2);
        assertThat(catalog.activeCount()).isEqualTo(2);
        assertThat(catalog.generatedAt()).isNotBlank();

        CatalogProduct pollo = byId(catalog, "A");

        assertThat(pollo.id()).isEqualTo("A");
        assertThat(pollo.externalId()).isEqualTo("A");
        assertThat(pollo.name()).isEqualTo("Pollo entero");
        assertThat(pollo.mainCategory()).isEqualTo("Carne");
        assertThat(pollo.visualGroup()).isEqualTo("Aves");
        assertThat(pollo.imageUrl()).isEqualTo("http://img/a.jpg");
        assertThat(pollo.format()).isEqualTo("1 kg");
        assertThat(pollo.price()).isEqualByComparingTo("5.20");
        assertThat(pollo.catalogOrder()).isZero();
        assertThat(pollo.active()).isTrue();
        assertThat(pollo.source()).isEqualTo("MERCADONA");
        assertThat(pollo.firstSeenAt()).isNotBlank();
        assertThat(pollo.previousPrice()).isNull();

        // Anclaje inicial del histórico de precios.
        assertThat(pollo.priceHistory()).hasSize(1);
        assertThat(pollo.priceHistory().get(0).price()).isEqualByComparingTo("5.20");
        assertThat(pollo.priceHistory().get(0).recordedAt()).isNotBlank();

        CatalogProduct manzana = byId(catalog, "B");
        assertThat(manzana.mainCategory()).isEqualTo("Frutas y verduras");
        assertThat(manzana.visualGroup()).isEqualTo("Fruta");
        assertThat(manzana.catalogOrder()).isEqualTo(1);
    }

    @Test
    void export_isIdempotent() throws Exception {

        StubProvider provider = new StubProvider("MERCADONA");
        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "10.00"),
                product("B", "Manzanas", "Manzana y pera", "img", "1 kg", "2.00")));

        exporter(provider, fixedClock("2026-01-01T00:00:00Z")).export();

        String firstSeenA = byId(readCatalog(), "A").firstSeenAt();

        CatalogExportResult second =
                exporter(provider, fixedClock("2026-02-01T00:00:00Z")).export();

        assertThat(second.sync().getCreated()).isZero();
        assertThat(second.sync().getUpdated()).isZero();
        assertThat(second.sync().getDeactivated()).isZero();
        assertThat(second.sync().getUnchanged()).isEqualTo(2);

        CatalogProduct a = byId(readCatalog(), "A");

        assertThat(a.firstSeenAt()).isEqualTo(firstSeenA);
        assertThat(a.firstSeenAt()).isEqualTo("2026-01-01T00:00:00Z");
        assertThat(a.previousPrice()).isNull();
        assertThat(a.priceChangedAt()).isNull();
        assertThat(a.lastSyncedAt()).isEqualTo("2026-02-01T00:00:00Z");
    }

    @Test
    void export_detectsPriceChange() throws Exception {

        StubProvider provider = new StubProvider("MERCADONA");
        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "10.00")));

        exporter(provider, fixedClock("2026-01-01T00:00:00Z")).export();

        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "12.00")));

        CatalogExportResult result =
                exporter(provider, fixedClock("2026-02-01T00:00:00Z")).export();

        assertThat(result.sync().getUpdated()).isEqualTo(1);
        assertThat(result.sync().getUnchanged()).isZero();

        CatalogProduct a = byId(readCatalog(), "A");

        assertThat(a.price()).isEqualByComparingTo("12.00");
        assertThat(a.previousPrice()).isEqualByComparingTo("10.00");
        assertThat(a.priceChangedAt()).isEqualTo("2026-02-01T00:00:00Z");
        assertThat(a.firstSeenAt()).isEqualTo("2026-01-01T00:00:00Z");

        // El cambio de precio añade un punto al histórico.
        assertThat(a.priceHistory()).hasSize(2);
        assertThat(a.priceHistory().get(1).price()).isEqualByComparingTo("12.00");
    }

    @Test
    void export_accumulatesPriceHistoryAcrossRuns() throws Exception {

        StubProvider provider = new StubProvider("MERCADONA");

        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "10.00")));
        exporter(provider, fixedClock("2026-01-01T00:00:00Z")).export();

        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "12.00")));
        exporter(provider, fixedClock("2026-02-01T00:00:00Z")).export();

        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "9.50")));
        exporter(provider, fixedClock("2026-03-01T00:00:00Z")).export();

        CatalogProduct a = byId(readCatalog(), "A");

        assertThat(a.priceHistory()).hasSize(3);
        assertThat(a.priceHistory().get(0).price()).isEqualByComparingTo("10.00");
        assertThat(a.priceHistory().get(1).price()).isEqualByComparingTo("12.00");
        assertThat(a.priceHistory().get(2).price()).isEqualByComparingTo("9.50");
        assertThat(a.firstSeenAt()).isEqualTo("2026-01-01T00:00:00Z");
    }

    @Test
    void export_trimsPriceHistoryOlderThanRetentionWindow() throws Exception {

        StubProvider provider = new StubProvider("MERCADONA");

        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "10.00")));
        exporter(provider, fixedClock("2026-01-01T00:00:00Z")).export();

        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "12.00")));
        exporter(provider, fixedClock("2026-02-15T00:00:00Z")).export();

        // Más de 60 días después del primer punto: el más antiguo queda fuera.
        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "11.00")));
        exporter(provider, fixedClock("2026-03-15T00:00:00Z")).export();

        CatalogProduct a = byId(readCatalog(), "A");

        // El primer punto (10.00 el 01-01) queda fuera de la ventana de 60 días.
        assertThat(a.priceHistory()).hasSize(2);
        assertThat(a.priceHistory().get(0).price()).isEqualByComparingTo("12.00");
        assertThat(a.priceHistory().get(1).price()).isEqualByComparingTo("11.00");
    }

    @Test
    void export_carriesForwardLastPriceChangeWhenUnchanged() throws Exception {

        StubProvider provider = new StubProvider("MERCADONA");

        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "10.00")));
        exporter(provider, fixedClock("2026-01-01T00:00:00Z")).export();

        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "12.00")));
        exporter(provider, fixedClock("2026-02-01T00:00:00Z")).export();

        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "12.00")));
        CatalogExportResult third =
                exporter(provider, fixedClock("2026-03-01T00:00:00Z")).export();

        assertThat(third.sync().getUpdated()).isZero();
        assertThat(third.sync().getUnchanged()).isEqualTo(1);

        CatalogProduct a = byId(readCatalog(), "A");

        assertThat(a.previousPrice()).isEqualByComparingTo("10.00");
        assertThat(a.priceChangedAt()).isEqualTo("2026-02-01T00:00:00Z");
    }

    @Test
    void export_deactivatesMissingKeepsThemAndAddsNew() throws Exception {

        StubProvider provider = new StubProvider("MERCADONA");
        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "10.00"),
                product("B", "Manzanas", "Manzana y pera", "img", "1 kg", "2.00"),
                product("C", "Leche", "Leche entera", "img", "1 l", "0.90")));

        exporter(provider, fixedClock("2026-01-01T00:00:00Z")).export();

        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "10.00"),
                product("D", "Arroz", "Arroz", "img", "1 kg", "1.30")));

        CatalogExportResult result =
                exporter(provider, fixedClock("2026-02-01T00:00:00Z")).export();

        assertThat(result.sync().getCreated()).isEqualTo(1);
        assertThat(result.sync().getDeactivated()).isEqualTo(2);
        assertThat(result.sync().getUnchanged()).isEqualTo(1);

        CatalogFile catalog = readCatalog();

        assertThat(catalog.productCount()).isEqualTo(4);
        assertThat(catalog.activeCount()).isEqualTo(2);

        CatalogProduct b = byId(catalog, "B");

        assertThat(b.active()).isFalse();
        assertThat(b.name()).isEqualTo("Manzanas");
        assertThat(b.firstSeenAt()).isEqualTo("2026-01-01T00:00:00Z");

        CatalogProduct d = byId(catalog, "D");

        assertThat(d.active()).isTrue();
        assertThat(d.firstSeenAt()).isEqualTo("2026-02-01T00:00:00Z");
    }

    @Test
    void export_reactivatesReappearingProductWithoutCountingAsUpdate() throws Exception {

        StubProvider provider = new StubProvider("MERCADONA");
        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "10.00"),
                product("B", "Manzanas", "Manzana y pera", "img", "1 kg", "2.00")));
        exporter(provider, fixedClock("2026-01-01T00:00:00Z")).export();

        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "10.00")));
        exporter(provider, fixedClock("2026-02-01T00:00:00Z")).export();

        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "10.00"),
                product("B", "Manzanas", "Manzana y pera", "img", "1 kg", "2.00")));
        exporter(provider, fixedClock("2026-03-01T00:00:00Z")).export();

        CatalogProduct b = byId(readCatalog(), "B");

        assertThat(b.active()).isTrue();
        assertThat(b.firstSeenAt()).isEqualTo("2026-01-01T00:00:00Z");
    }

    @Test
    void export_doesNotOverwriteWhenProviderFails() throws Exception {

        StubProvider provider = new StubProvider("MERCADONA");
        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "10.00")));
        exporter(provider, fixedClock("2026-01-01T00:00:00Z")).export();

        Path file = tempDir.resolve(CatalogExporter.CATALOG_FILE_NAME);
        byte[] before = Files.readAllBytes(file);

        provider.failWith(new RuntimeException("Mercadona no disponible"));

        CatalogExportResult result =
                exporter(provider, fixedClock("2026-02-01T00:00:00Z")).export();

        assertThat(result.success()).isFalse();
        assertThat(result.sync()).isNull();
        assertThat(Files.readAllBytes(file)).isEqualTo(before);
    }

    @Test
    void export_doesNotOverwriteWhenProviderReturnsEmpty() throws Exception {

        StubProvider provider = new StubProvider("MERCADONA");
        provider.returnProducts(List.of(
                product("A", "Pollo", "Pollo", "img", "1 kg", "10.00")));
        exporter(provider).export();

        Path file = tempDir.resolve(CatalogExporter.CATALOG_FILE_NAME);
        byte[] before = Files.readAllBytes(file);

        provider.returnProducts(List.of());

        CatalogExportResult result = exporter(provider).export();

        assertThat(result.success()).isFalse();
        assertThat(Files.readAllBytes(file)).isEqualTo(before);
    }

    /*
     * Proveedor de prueba en memoria: permite simular un catálogo válido o un
     * fallo de la fuente sin tocar la red.
     */
    private static final class StubProvider implements ProductProvider {

        private final String source;
        private List<Product> products = List.of();
        private RuntimeException failure;

        StubProvider(String source) {
            this.source = source;
        }

        void returnProducts(List<Product> products) {
            this.products = products;
            this.failure = null;
        }

        void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public List<Product> getProducts() {

            if (failure != null) {
                throw failure;
            }

            return products;
        }

        @Override
        public String getSource() {
            return source;
        }
    }
}
