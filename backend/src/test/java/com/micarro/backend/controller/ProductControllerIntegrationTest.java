package com.micarro.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.micarro.backend.entity.Product;
import com.micarro.backend.repository.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

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
        product.setCatalogOrder(catalogOrder);
        product.setSource("MERCADONA");
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
}
