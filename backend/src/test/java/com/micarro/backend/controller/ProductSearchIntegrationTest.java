package com.micarro.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.micarro.backend.repository.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Product product(
            String name,
            String category,
            String mainCategory,
            String visualGroup,
            String price,
            int catalogOrder) {

        Product product = new Product();
        product.setExternalId("search-" + UUID.randomUUID());
        product.setName(name);
        product.setCategory(category);
        product.setMainCategory(mainCategory);
        product.setVisualGroup(visualGroup);
        product.setPrice(price == null ? null : new BigDecimal(price));
        product.setCatalogOrder(catalogOrder);
        product.setSource("MERCADONA");
        product.setActive(true);

        return productRepository.save(product);
    }

    @Test
    void search_carne_prioritizesMainCategoryCarne() throws Exception {

        Product carne = product("Filete de ternera", "Vacuno", "Carne", "Vacuno", "8", 1);
        Product chili = product("Chili con carne", "Otras salsas", "Aceites, salsas y especias", "Salsas y vinagres", "2", 2);
        Product sazonador = product("Sazonador para carne", "Sazonadores", "Aceites, salsas y especias", "Especias y sal", "1", 3);

        mockMvc.perform(get("/api/products").param("search", "carne"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].id").value(carne.getId()))
                .andExpect(jsonPath("$.content[1].id").value(chili.getId()))
                .andExpect(jsonPath("$.content[2].id").value(sazonador.getId()));
    }

    @Test
    void search_exactMatchWinsOverPartial() throws Exception {

        Product exact = product("Filete", "Vacuno", "Carne", "Carnes rojas", "8", 1);
        Product partial = product("Picadillo", "Productos vacunos", "Carne", "Carnes rojas", "6", 2);

        mockMvc.perform(get("/api/products").param("search", "vacuno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(exact.getId()))
                .andExpect(jsonPath("$.content[1].id").value(partial.getId()));
    }

    @Test
    void search_isAccentInsensitive() throws Exception {

        Product pimenton = product("Pimentón dulce", "Especias", "Aceites, salsas y especias", "Especias y sal", "1", 1);

        // Búsqueda sin acento encuentra datos con acento.
        mockMvc.perform(get("/api/products").param("search", "pimenton"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(pimenton.getId()));
    }

    @Test
    void search_manualPriceSortIgnoresRanking() throws Exception {

        Product carne = product("Filete de ternera", "Vacuno", "Carne", "Vacuno", "8", 1);
        Product chili = product("Chili con carne", "Otras salsas", "Aceites, salsas y especias", "Salsas y vinagres", "3", 2);

        mockMvc.perform(get("/api/products")
                        .param("search", "carne")
                        .param("sortBy", "price-asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(chili.getId()))
                .andExpect(jsonPath("$.content[1].id").value(carne.getId()));
    }

    @Test
    void search_paginationIsDeterministicWithTies() throws Exception {

        int size = 10;
        int count = size + 5;

        List<Long> expected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Product product = product(
                    "Producto determinista " + i,
                    "Categoria",
                    "Carne",
                    "Vacuno",
                    "1",
                    1
            );
            expected.add(product.getId());
        }
        expected.sort(Comparator.naturalOrder()); // mismo catalogOrder -> id ASC

        List<Long> seen = new ArrayList<>();
        int page = 0;
        boolean last = false;

        while (!last) {
            String body = mockMvc.perform(get("/api/products")
                            .param("search", "determinista")
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

        assertThat(seen).hasSize(count);
        assertThat(new HashSet<>(seen)).hasSize(count);
        assertThat(seen).isEqualTo(expected);
    }

    @Test
    void search_leche_prioritizesMilkOverCheese() throws Exception {

        Product leche = product("Leche entera Hacendado", "Leche", "Leche, huevos y lácteos", "Leche", "1", 1);
        Product queso = product("Queso manchego", "Queso curado", "Leche, huevos y lácteos", "Quesos", "3", 2);

        // El queso solo coincide por compartir mainCategory: debe ir después
        // de la leche, que coincide en visualGroup/category.
        mockMvc.perform(get("/api/products").param("search", "leche"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(leche.getId()))
                .andExpect(jsonPath("$.content[1].id").value(queso.getId()));
    }

    @Test
    void search_patataCocida_findsPluralProduct() throws Exception {

        Product patatas = product("Patatas cocidas Hacendado", "Patata", "Frutas y verduras", "Patatas", "1", 1);

        mockMvc.perform(get("/api/products").param("search", "patata cocida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(patatas.getId()));
    }

    @Test
    void search_tomateTriturado_findsPluralProduct() throws Exception {

        Product tomates = product("Tomates triturados", "Tomate", "Frutas y verduras", "Verdura", "1", 1);

        mockMvc.perform(get("/api/products").param("search", "tomate triturado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(tomates.getId()));
    }

    @Test
    void search_galletaChocolate_requiresBothTokens() throws Exception {

        Product galletasChocolate = product("Galletas de chocolate", "Galletas desayuno", "Desayuno y dulces", "Galletas", "1", 1);
        product("Chocolate negro puro", "Chocolate negro", "Desayuno y dulces", "Cacao y chocolate", "2", 2);

        mockMvc.perform(get("/api/products").param("search", "galleta chocolate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(galletasChocolate.getId()));
    }

    @Test
    void search_leche_prioritizesNormalMilkOverPreparadosAndInfant() throws Exception {

        Product preparado = product("Preparado lácteo con cereales y frutas Peques 3 Puleva", "Leche", "Leche, huevos y lácteos", "Leche", "2", 1);
        Product infantil = product("Leche para lactantes en polvo 1 Nativa Nestlé", "Leche en polvo", "Leche, huevos y lácteos", "Leche en polvo", "3", 2);
        Product crema = product("Crema de leche para café Campina", "Leche condensada y otros", "Leche, huevos y lácteos", "Leche condensada y otros", "4", 3);
        Product leche = product("Leche semidesnatada Hacendado", "Leche semidesnatada", "Leche, huevos y lácteos", "Leche", "1", 10);

        // Leche normal (grupo "Leche" + nombre) primero; preparado con nombre sin
        // "leche" después; infantil y crema (grupos propios) al final.
        mockMvc.perform(get("/api/products").param("search", "leche"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(leche.getId()))
                .andExpect(jsonPath("$.content[1].id").value(preparado.getId()))
                .andExpect(jsonPath("$.content[2].id").value(infantil.getId()))
                .andExpect(jsonPath("$.content[3].id").value(crema.getId()));
    }

    @Test
    void search_lecheEnPolvo_prioritizesInfantFormula() throws Exception {

        Product infantil = product("Leche para lactantes en polvo 1 Nativa Nestlé", "Leche en polvo", "Leche, huevos y lácteos", "Leche en polvo", "3", 1);
        product("Leche semidesnatada Hacendado", "Leche semidesnatada", "Leche, huevos y lácteos", "Leche", "1", 10);

        mockMvc.perform(get("/api/products").param("search", "leche en polvo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(infantil.getId()));
    }

    @Test
    void search_lecheCondensada_prioritizesCondensed() throws Exception {

        Product crema = product("Crema de leche para café Campina", "Leche condensada y otros", "Leche, huevos y lácteos", "Leche condensada y otros", "4", 1);
        product("Leche semidesnatada Hacendado", "Leche semidesnatada", "Leche, huevos y lácteos", "Leche", "1", 10);

        mockMvc.perform(get("/api/products").param("search", "leche condensada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(crema.getId()));
    }
}