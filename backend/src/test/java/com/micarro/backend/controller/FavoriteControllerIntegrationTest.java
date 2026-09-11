package com.micarro.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class FavoriteControllerIntegrationTest {

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

    private Product createProduct(String name) {

        Product product = new Product();
        product.setName(name);
        product.setExternalId("test-" + UUID.randomUUID());
        product.setPrice(new BigDecimal("1.50"));

        return productRepository.save(product);
    }

    private String uniqueEmail() {
        return "fav-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void addListAndRemoveFavorite() throws Exception {

        String token = registerAndGetToken(uniqueEmail());
        Product product = createProduct("Pollo test");

        mockMvc.perform(post("/api/favorites/" + product.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(product.getId()));

        mockMvc.perform(delete("/api/favorites/" + product.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void addFavorite_doesNotDuplicate() throws Exception {

        String token = registerAndGetToken(uniqueEmail());
        Product product = createProduct("Arroz test");

        mockMvc.perform(post("/api/favorites/" + product.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/favorites/" + product.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void addFavorite_returnsNotFoundForUnknownProduct() throws Exception {

        String token = registerAndGetToken(uniqueEmail());

        mockMvc.perform(post("/api/favorites/999999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void favoritesRequireAuthentication() throws Exception {

        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isUnauthorized());
    }
}
