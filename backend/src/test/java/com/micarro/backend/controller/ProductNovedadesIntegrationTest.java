package com.micarro.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.micarro.backend.entity.Product;
import com.micarro.backend.entity.ProductPriceHistory;
import com.micarro.backend.repository.ProductPriceHistoryRepository;
import com.micarro.backend.repository.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductNovedadesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPriceHistoryRepository priceHistoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Product product(String name, String price, Instant firstSeenAt) {

        Product product = new Product();
        product.setExternalId("novedades-" + UUID.randomUUID());
        product.setName(name);
        product.setSource("MERCADONA");
        product.setActive(true);
        product.setPrice(price == null ? null : new BigDecimal(price));
        product.setFirstSeenAt(firstSeenAt);

        return productRepository.save(product);
    }

    private void history(Product product, String price, Instant recordedAt) {

        ProductPriceHistory history = new ProductPriceHistory();
        history.setProductId(product.getId());
        history.setPrice(new BigDecimal(price));
        history.setRecordedAt(recordedAt);

        priceHistoryRepository.save(history);
    }

    @Test
    void new_returnsProductsSeenInLast30Days() throws Exception {

        Product recent = product("Reciente", "5", Instant.now());
        product("Antiguo", "5", Instant.now().minus(60, java.time.temporal.ChronoUnit.DAYS));

        mockMvc.perform(get("/api/products/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(recent.getId()))
                .andExpect(jsonPath("$.content[0].previousPrice").doesNotExist());
    }

    @Test
    void priceDrops_returnsDropWithDifferenceAndPercent() throws Exception {

        Product drop = product("En oferta", "7", Instant.now().minus(60, java.time.temporal.ChronoUnit.DAYS));
        history(drop, "10", Instant.now().minus(40, java.time.temporal.ChronoUnit.DAYS));

        Product stable = product("Estable", "9", Instant.now().minus(60, java.time.temporal.ChronoUnit.DAYS));
        history(stable, "9", Instant.now().minus(40, java.time.temporal.ChronoUnit.DAYS));

        mockMvc.perform(get("/api/products/price-drops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(drop.getId()))
                .andExpect(jsonPath("$.content[0].previousPrice").value(10.0))
                .andExpect(jsonPath("$.content[0].currentPrice").value(7.0))
                .andExpect(jsonPath("$.content[0].difference").value(-3.0))
                .andExpect(jsonPath("$.content[0].differencePercent").value(-30.00));
    }

    @Test
    void priceRaises_returnsRiseWithDifferenceAndPercent() throws Exception {

        Product rise = product("Más caro", "12", Instant.now().minus(60, java.time.temporal.ChronoUnit.DAYS));
        history(rise, "10", Instant.now().minus(40, java.time.temporal.ChronoUnit.DAYS));

        mockMvc.perform(get("/api/products/price-raises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(rise.getId()))
                .andExpect(jsonPath("$.content[0].differencePercent").value(20.00));
    }

    @Test
    void priceDrops_excludesProductsWithoutEnoughHistory() throws Exception {

        // Sin fila de historial <= cutoff (30 días): no debe aparecer ni como
        // bajada ni como subida.
        Product noHistory = product("Sin histórico", "8", Instant.now().minus(60, java.time.temporal.ChronoUnit.DAYS));
        history(noHistory, "8", Instant.now());

        mockMvc.perform(get("/api/products/price-drops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(get("/api/products/price-raises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void priceDrops_ordersByBiggestPercentFirst() throws Exception {

        Product bigDrop = product("Gran bajada", "5", Instant.now().minus(60, java.time.temporal.ChronoUnit.DAYS));
        history(bigDrop, "10", Instant.now().minus(40, java.time.temporal.ChronoUnit.DAYS));

        Product smallDrop = product("Pequeña bajada", "9", Instant.now().minus(60, java.time.temporal.ChronoUnit.DAYS));
        history(smallDrop, "10", Instant.now().minus(40, java.time.temporal.ChronoUnit.DAYS));

        mockMvc.perform(get("/api/products/price-drops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(bigDrop.getId()))
                .andExpect(jsonPath("$.content[1].id").value(smallDrop.getId()));
    }

    @Test
    void priceDrops_paginates() throws Exception {

        for (int i = 0; i < 3; i++) {
            Product drop = product("Oferta " + i, "8", Instant.now().minus(60, java.time.temporal.ChronoUnit.DAYS));
            history(drop, "10", Instant.now().minus(40, java.time.temporal.ChronoUnit.DAYS));
        }

        mockMvc.perform(get("/api/products/price-drops").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    void newProducts_excludesRecentReferenceWhenHistoricalEquivalentExists() throws Exception {

        // Referencia histórica anterior (firstSeenAt NULL, id menor, otro externalId).
        Product historical = product("Refresco Fanta naranja", "1.85", null);

        // Referencia reciente con la misma identidad estricta.
        Product recent = product("Refresco Fanta naranja", "1.55", Instant.now());

        mockMvc.perform(get("/api/products/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        // La fila histórica no se modifica.
        assertThat(historical.isActive()).isTrue();
    }

    @Test
    void newProducts_excludesRecentReferenceWhenInactivePriorExists() throws Exception {

        Product historical = product("Refresco Fanta naranja", "1.85", null);
        historical.setActive(false);
        productRepository.save(historical);

        product("Refresco Fanta naranja", "1.55", Instant.now());

        mockMvc.perform(get("/api/products/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void newProducts_keepsGenuinelyNewProductWithoutHistoricalReference() throws Exception {

        Product recent = product("Pera Limonera", "2.10", Instant.now());

        mockMvc.perform(get("/api/products/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(recent.getId()));
    }

    @Test
    void newProducts_doesNotExcludeSimilarProductWithDifferentCategory() throws Exception {

        Product historical = product("Salsa Sweet Relish", "2.00", null);
        historical.setCategory("Aceites");
        historical.setFormat("Tarro");
        productRepository.save(historical);

        Product recent = product("Salsa Sweet Relish", "2.00", Instant.now());
        recent.setCategory("Conservas");
        recent.setFormat("Tarro");
        productRepository.save(recent);

        // Identidad estricta: la categoría difiere, así que NO se excluye.
        mockMvc.perform(get("/api/products/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(recent.getId()));

        // Ninguna fila se fusiona ni modifica.
        assertThat(historical.getCategory()).isEqualTo("Aceites");
        assertThat(recent.getCategory()).isEqualTo("Conservas");
    }

    @Test
    void newProducts_exclusionDoesNotBreakPriceHistory() throws Exception {

        Product historical = product("Refresco Fanta naranja", "1.85", null);
        history(historical, "1.85", Instant.now());

        Product recent = product("Refresco Fanta naranja", "1.55", Instant.now());
        history(recent, "1.55", Instant.now());

        mockMvc.perform(get("/api/products/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        // El historial de precios sigue asociado a ambas filas (no se borra).
        assertThat(priceHistoryRepository.findAll()).hasSize(2);
    }

    @Test
    void newProducts_paginationIsDeterministicWithTies() throws Exception {

        int size = 10;
        int count = size + 5; // más productos que el tamaño de página con el MISMO firstSeenAt
        Instant same = Instant.now();

        List<Long> expected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Product product = product("Producto determinista " + i, "1.00", same);
            expected.add(product.getId());
        }
        // Todos comparten firstSeenAt -> el orden es firstSeenAt DESC (igual), id ASC.
        expected.sort(Comparator.naturalOrder());

        List<Long> seen = new ArrayList<>();
        int page = 0;
        boolean last = false;

        while (!last) {
            String body = mockMvc.perform(get("/api/products/new")
                            .param("size", String.valueOf(size))
                            .param("page", String.valueOf(page)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            JsonNode root = objectMapper.readTree(body);
            for (JsonNode item : root.path("content")) {
                seen.add(item.get("id").asLong());
            }
            last = root.path("last").asBoolean();
            page++;
        }

        // Ningún id se repite entre páginas y no falta ningún producto.
        assertThat(seen).hasSize(count);
        assertThat(new HashSet<>(seen)).hasSize(count);

        // Orden determinista global: mismo firstSeenAt -> id ASC.
        assertThat(seen).isEqualTo(expected);
    }
}