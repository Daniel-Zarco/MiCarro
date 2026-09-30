package com.micarro.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
class SavedPlanControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    private String registerAndGetToken(String email) throws Exception {

        String body = """
                {
                  "name": "Test",
                  "email": "%s",
                  "password": "password123"
                }
                """.formatted(email);

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(response, "$.token");
    }

    private Product createProduct(String name, String price) {

        Product product = new Product();
        product.setName(name);
        product.setBrand("Marca");
        product.setFormat("1 ud.");
        product.setExternalId("saved-plan-" + UUID.randomUUID());
        product.setPrice(new BigDecimal(price));

        return productRepository.save(product);
    }

    private String uniqueEmail() {
        return "plan-" + UUID.randomUUID() + "@example.com";
    }

    private String planBody(long productId, int quantity) {

        return """
                {
                  "name": "Mi plan",
                  "items": [
                    { "productId": %d, "quantity": %d }
                  ]
                }
                """.formatted(productId, quantity);
    }

    @Test
    void createListGetAndDeletePlan() throws Exception {

        String token = registerAndGetToken(uniqueEmail());
        Product product = createProduct("Legumbres", "1.50");

        // El cliente solo envía name + productId + quantity; el snapshot se
        // construye en el backend desde el catálogo.
        String created = mockMvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planBody(product.getId(), 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mi plan"))
                .andExpect(jsonPath("$.items[0].productName").value("Legumbres"))
                .andExpect(jsonPath("$.items[0].brand").value("Marca"))
                .andExpect(jsonPath("$.items[0].format").value("1 ud."))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(1.50))
                .andExpect(jsonPath("$.items[0].subtotal").value(3.00))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long planId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(get("/api/plans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(planId))
                .andExpect(jsonPath("$[0].name").value("Mi plan"))
                .andExpect(jsonPath("$[0].itemCount").value(1))
                .andExpect(jsonPath("$[0].total").value(3.00));

        mockMvc.perform(get("/api/plans/" + planId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value("Legumbres"));

        mockMvc.perform(delete("/api/plans/" + planId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/plans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void detailReturnsHistoricalAndCurrentPrice() throws Exception {

        String token = registerAndGetToken(uniqueEmail());
        Product product = createProduct("Arroz", "1.20");

        String created = mockMvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planBody(product.getId(), 3)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long planId = ((Number) JsonPath.read(created, "$.id")).longValue();

        // El producto cambia de precio tras guardar el plan.
        product.setPrice(new BigDecimal("1.80"));
        productRepository.save(product);

        mockMvc.perform(get("/api/plans/" + planId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice").value(1.20))
                .andExpect(jsonPath("$.items[0].subtotal").value(3.60))
                .andExpect(jsonPath("$.items[0].currentProduct.price").value(1.80));
    }

    @Test
    void createRejectsUnknownProduct() throws Exception {

        String token = registerAndGetToken(uniqueEmail());

        mockMvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planBody(999999999L, 1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsInvalidRequest() throws Exception {

        String token = registerAndGetToken(uniqueEmail());
        Product product = createProduct("Pan", "0.90");

        // Sin nombre, sin items o con cantidad no positiva.
        mockMvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   ",
                                  "items": [{ "productId": %d, "quantity": 1 }]
                                }
                                """.formatted(product.getId())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mi plan",
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mi plan",
                                  "items": [{ "productId": %d, "quantity": 0 }]
                                }
                                """.formatted(product.getId())))
                .andExpect(status().isBadRequest());

        // Nombre de más de 80 caracteres.
        mockMvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "items": [{ "productId": %d, "quantity": 1 }]
                                }
                                """.formatted("a".repeat(81), product.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void otherUserCannotAccessPlan() throws Exception {

        String ownerToken = registerAndGetToken(uniqueEmail());
        String otherToken = registerAndGetToken(uniqueEmail());
        Product product = createProduct("Pasta", "0.85");

        String created = mockMvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planBody(product.getId(), 1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long planId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(get("/api/plans/" + planId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/plans/" + planId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void renamePlanUpdatesName() throws Exception {

        String token = registerAndGetToken(uniqueEmail());
        Product product = createProduct("Pan", "0.90");

        String created = mockMvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planBody(product.getId(), 1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long planId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(patch("/api/plans/" + planId + "/name")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Compra navideña" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(planId))
                .andExpect(jsonPath("$.name").value("Compra navideña"));

        mockMvc.perform(get("/api/plans/" + planId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Compra navideña"));
    }

    @Test
    void renameRejectsInvalidName() throws Exception {

        String token = registerAndGetToken(uniqueEmail());
        Product product = createProduct("Pan", "0.90");

        String created = mockMvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planBody(product.getId(), 1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long planId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(patch("/api/plans/" + planId + "/name")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "   " }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/plans/" + planId + "/name")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "%s" }
                                """.formatted("a".repeat(81))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void renameRejectsOtherUsersPlan() throws Exception {

        String ownerToken = registerAndGetToken(uniqueEmail());
        String otherToken = registerAndGetToken(uniqueEmail());
        Product product = createProduct("Pan", "0.90");

        String created = mockMvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planBody(product.getId(), 1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long planId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(patch("/api/plans/" + planId + "/name")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Robado" }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void renameRequiresAuthentication() throws Exception {

        mockMvc.perform(patch("/api/plans/1/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Nuevo" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void plansRequireAuthentication() throws Exception {

        mockMvc.perform(get("/api/plans"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mi plan",
                                  "items": [{ "productId": 1, "quantity": 1 }]
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}