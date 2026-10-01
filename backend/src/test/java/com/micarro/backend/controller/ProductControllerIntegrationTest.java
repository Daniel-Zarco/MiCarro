package com.micarro.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.micarro.backend.entity.Product;
import com.micarro.backend.repository.ProductRepository;
import com.micarro.backend.service.MainCategoryMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MainCategoryMapper mainCategoryMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private String uniqueExternalId() {
        return "test-" + UUID.randomUUID();
    }

    private Product product(String name, boolean active) {

        Product product = new Product();
        product.setExternalId(uniqueExternalId());
        product.setName(name);
        product.setSource("MERCADONA");
        product.setActive(active);

        return productRepository.save(product);
    }

    private Product product(String name, String category, boolean active) {

        Product product = new Product();
        product.setExternalId(uniqueExternalId());
        product.setName(name);
        product.setCategory(category);
        product.setMainCategory(mainCategoryMapper.map(category));
        product.setSource("MERCADONA");
        product.setActive(active);

        return productRepository.save(product);
    }

    private Product product(
            String name,
            String category,
            int catalogOrder,
            boolean active) {

        Product product = new Product();
        product.setExternalId(uniqueExternalId());
        product.setName(name);
        product.setCategory(category);
        product.setMainCategory(mainCategoryMapper.map(category));
        product.setCatalogOrder(catalogOrder);
        product.setSource("MERCADONA");
        product.setActive(active);

        return productRepository.save(product);
    }

    private Product product(
            String name,
            BigDecimal price,
            boolean active) {

        Product product = new Product();
        product.setExternalId(uniqueExternalId());
        product.setName(name);
        product.setPrice(price);
        product.setSource("MERCADONA");
        product.setActive(active);

        return productRepository.save(product);
    }

    private Product product(
            String name,
            Instant firstSeenAt,
            boolean active) {

        Product product = new Product();
        product.setExternalId(uniqueExternalId());
        product.setName(name);
        product.setSource("MERCADONA");
        product.setFirstSeenAt(firstSeenAt);
        product.setActive(active);

        return productRepository.save(product);
    }

    private String createBody(String externalId) {
        return """
                {
                  "externalId": "%s",
                  "name": "Producto test",
                  "brand": "Marca",
                  "category": "Categoria",
                  "imageUrl": "http://example.com/p.jpg",
                  "format": "1 kg",
                  "price": 9.99
                }
                """.formatted(externalId);
    }

    private Long createProduct(String externalId) throws Exception {

        String created = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(externalId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return ((Number) JsonPath.read(created, "$.id")).longValue();
    }

    @Test
    void createGetAndListUseOwnDtos() throws Exception {

        String externalId = uniqueExternalId();

        String created = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(externalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Producto test"))
                .andExpect(jsonPath("$.externalId").value(externalId))
                .andExpect(jsonPath("$.format").value("1 kg"))
                .andExpect(jsonPath("$.price").value(9.99))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Producto test"));

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.first").isBoolean())
                .andExpect(jsonPath("$.last").isBoolean());
    }

    @Test
    void getProducts_returnsProductsSortedByCatalogOrder() throws Exception {

        productRepository.deleteAll();

        product("Zanahoria", "Verdura", 3, true);
        product("Manzana", "Fruta", 1, true);
        product("Arándanos", "Fruta", 0, true);
        product("Brócoli", "Verdura", 2, true);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Arándanos"))
                .andExpect(jsonPath("$.content[1].name").value("Manzana"))
                .andExpect(jsonPath("$.content[2].name").value("Brócoli"))
                .andExpect(jsonPath("$.content[3].name").value("Zanahoria"));
    }

    @Test
    void getProducts_sortsByPriceAscendingWithNullsLast() throws Exception {

        productRepository.deleteAll();

        product("Sin precio", (BigDecimal) null, true);
        product("Caro", new BigDecimal("9.99"), true);
        product("Barato", new BigDecimal("1.50"), true);
        product("Medio", new BigDecimal("5.00"), true);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "price-asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Barato"))
                .andExpect(jsonPath("$.content[1].name").value("Medio"))
                .andExpect(jsonPath("$.content[2].name").value("Caro"))
                .andExpect(jsonPath("$.content[3].name").value("Sin precio"));
    }

    @Test
    void getProducts_sortsByPriceDescendingWithNullsLast() throws Exception {

        productRepository.deleteAll();

        product("Sin precio", (BigDecimal) null, true);
        product("Caro", new BigDecimal("9.99"), true);
        product("Barato", new BigDecimal("1.50"), true);
        product("Medio", new BigDecimal("5.00"), true);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "price-desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Caro"))
                .andExpect(jsonPath("$.content[1].name").value("Medio"))
                .andExpect(jsonPath("$.content[2].name").value("Barato"))
                .andExpect(jsonPath("$.content[3].name").value("Sin precio"));
    }

    @Test
    void getCategories_returnsDistinctMainCategoriesAlphabeticalWithCounts() throws Exception {

        productRepository.deleteAll();

        product("Manzanas", "Manzana y pera", 1, true);
        product("Peras", "Manzana y pera", 0, true);
        product("Pollo", "Pollo", 3, true);
        product("Lomo", "Chorizo", 2, true);
        product("Oculto", "Manzana y pera", 0, false);

        mockMvc.perform(get("/api/products/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Carne"))
                .andExpect(jsonPath("$[0].productCount").value(1))
                .andExpect(jsonPath("$[1].name").value("Charcutería"))
                .andExpect(jsonPath("$[1].productCount").value(1))
                .andExpect(jsonPath("$[2].name").value("Frutas y verduras"))
                .andExpect(jsonPath("$[2].productCount").value(2));
    }

    @Test
    void getProducts_filtersByMainCategoryWithPagination() throws Exception {

        productRepository.deleteAll();

        product("Manzanas", "Manzana y pera", 1, true);
        product("Peras", "Manzana y pera", 0, true);
        product("Pollo", "Pollo", 3, true);
        product("Oculta", "Manzana y pera", 0, false);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("category", "Frutas y verduras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Peras"))
                .andExpect(jsonPath("$.content[1].name").value("Manzanas"));

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("category", "Carne"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Pollo"));
    }

    @Test
    void getProducts_combinesCategoryAndSearch() throws Exception {

        productRepository.deleteAll();

        String token = UUID.randomUUID().toString();

        product("Manzana " + token, "Manzana y pera", 1, true);
        product("Otro " + token, "Pollo", 0, true);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("category", "Frutas y verduras")
                        .param("search", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Manzana " + token));
    }

    @Test
    void getGroups_returnsVisualGroupsWithCounts() throws Exception {

        productRepository.deleteAll();

        product("Manzanas", "Manzana y pera", 1, true);
        product("Peras", "Manzana y pera", 0, true);
        product("Pollo", "Pollo", 2, true);
        product("Cerdo", "Cerdo", 3, true);

        mockMvc.perform(get("/api/products/categories/Carne/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aves"))
                .andExpect(jsonPath("$[0].productCount").value(1))
                .andExpect(jsonPath("$[1].name").value("Cerdo"))
                .andExpect(jsonPath("$[1].productCount").value(1));

        mockMvc.perform(get("/api/products/categories/Frutas%20y%20verduras/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Fruta"))
                .andExpect(jsonPath("$[0].productCount").value(2));
    }

    @Test
    void getProducts_filtersByVisualGroup() throws Exception {

        productRepository.deleteAll();

        product("Manzanas", "Manzana y pera", 1, true);
        product("Peras", "Manzana y pera", 0, true);
        product("Pollo", "Pollo", 3, true);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("category", "Frutas y verduras")
                        .param("group", "Fruta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Peras"))
                .andExpect(jsonPath("$.content[1].name").value("Manzanas"));

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("category", "Frutas y verduras")
                        .param("group", "Verdura"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getProducts_combinesSearchAndVisualGroup() throws Exception {

        productRepository.deleteAll();

        String token = UUID.randomUUID().toString();

        product("Manzana " + token, "Manzana y pera", 1, true);
        product("Pera " + token, "Manzana y pera", 0, true);
        product("Pollo " + token, "Pollo", 2, true);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("category", "Frutas y verduras")
                        .param("group", "Fruta")
                        .param("search", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Pera " + token))
                .andExpect(jsonPath("$.content[1].name").value("Manzana " + token));
    }

    @Test
    void getProducts_sortsByNameCaseInsensitiveWithStableIdOrder() throws Exception {

        productRepository.deleteAll();

        product("manzana", "Manzana", 1, true);
        product("Pera", (BigDecimal) null, true);
        product("Arándanos", (BigDecimal) null, true);
        product("Manzana", (BigDecimal) null, true);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Arándanos"))
                .andExpect(jsonPath("$.content[1].name").value("manzana"))
                .andExpect(jsonPath("$.content[2].name").value("Manzana"))
                .andExpect(jsonPath("$.content[3].name").value("Pera"));
    }

    @Test
    void getProducts_sortsByNameDescendingCaseInsensitive() throws Exception {

        productRepository.deleteAll();

        product("manzana", "Manzana", 1, true);
        product("Pera", (BigDecimal) null, true);
        product("Arándanos", (BigDecimal) null, true);
        product("Manzana", (BigDecimal) null, true);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "name-desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Pera"))
                .andExpect(jsonPath("$.content[1].name").value("manzana"))
                .andExpect(jsonPath("$.content[2].name").value("Manzana"))
                .andExpect(jsonPath("$.content[3].name").value("Arándanos"));
    }

    @Test
    void getProducts_combinesSearchAndPriceSort() throws Exception {

        productRepository.deleteAll();

        String token = UUID.randomUUID().toString();

        product("B " + token, new BigDecimal("9.99"), true);
        product("A " + token, new BigDecimal("1.50"), true);
        product("C " + token, new BigDecimal("5.00"), true);
        product("Otro", new BigDecimal("0.50"), true);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("search", token)
                        .param("sortBy", "price-asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].name").value("A " + token))
                .andExpect(jsonPath("$.content[1].name").value("C " + token))
                .andExpect(jsonPath("$.content[2].name").value("B " + token));
    }

    @Test
    void getProducts_unknownSortByFallsBackToCatalogOrder() throws Exception {

        productRepository.deleteAll();

        product("Zanahoria", "Verdura", 3, true);
        product("Manzana", "Fruta", 1, true);
        product("Arándanos", "Fruta", 0, true);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "whatever"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Arándanos"))
                .andExpect(jsonPath("$.content[1].name").value("Manzana"))
                .andExpect(jsonPath("$.content[2].name").value("Zanahoria"));
    }

    @Test
    void updateProduct_returnsUpdatedResponse() throws Exception {

        String externalId = uniqueExternalId();
        Long id = createProduct(externalId);

        mockMvc.perform(put("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Producto actualizado",
                                  "brand": "Nueva marca",
                                  "category": "Nueva categoria"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Producto actualizado"))
                .andExpect(jsonPath("$.brand").value("Nueva marca"));
    }

    @Test
    void getProductById_notFound() throws Exception {

        mockMvc.perform(get("/api/products/999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void catalogOnlyShowsActiveProducts() throws Exception {

        String activeName = "Activo " + UUID.randomUUID();
        String inactiveName = "Inactivo " + UUID.randomUUID();

        product(activeName, true);
        Product inactive = product(inactiveName, false);

        mockMvc.perform(get("/api/products")
                        .param("search", activeName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/products")
                        .param("search", inactiveName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/products/" + inactive.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRecentProducts_returnsActiveProductsFromLastSixMonthsSortedByFirstSeenAtDesc()
            throws Exception {

        productRepository.deleteAll();

        Instant now = Instant.now();
        Instant fiveMonthsAgo = now.minus(5 * 30L, java.time.temporal.ChronoUnit.DAYS);
        Instant sevenMonthsAgo = now.minus(7 * 30L, java.time.temporal.ChronoUnit.DAYS);

        product("Reciente A", now.minusSeconds(60), true);
        product("Reciente B", fiveMonthsAgo, true);
        product("Antiguo", sevenMonthsAgo, true);
        product("Inactivo reciente", now.minusSeconds(60), false);

        mockMvc.perform(get("/api/products/recent")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Reciente A"))
                .andExpect(jsonPath("$.content[1].name").value("Reciente B"));
    }

    @Test
    void getProducts_paginationIsDeterministicWithCatalogOrderTies() throws Exception {

        int size = 10;
        int count = size + 5; // más productos que el tamaño de página con el MISMO catalogOrder
        int sameCatalogOrder = 7;

        List<Long> expected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Product product = product(
                    "Producto orden " + i,
                    "Categoria",
                    sameCatalogOrder,
                    true
            );
            expected.add(product.getId());
        }
        // Mismo catalogOrder -> el orden es catalogOrder ASC (igual), id ASC.
        expected.sort(Comparator.naturalOrder());

        List<Long> seen = new ArrayList<>();
        int page = 0;
        boolean last = false;

        while (!last) {
            String body = mockMvc.perform(get("/api/products")
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

        // Ningún id repetido entre páginas y ningún producto perdido.
        assertThat(seen).hasSize(count);
        assertThat(new HashSet<>(seen)).hasSize(count);

        // Orden determinista: mismo catalogOrder -> id ASC.
        assertThat(seen).isEqualTo(expected);
    }
}
