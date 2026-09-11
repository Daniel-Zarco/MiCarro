package com.micarro.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShoppingPlanHistoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private String historyBody() {
        return """
                {
                  "budget": 60,
                  "estimatedTotal": 20,
                  "mode": "BALANCED",
                  "items": [
                    {
                      "productId": 1,
                      "productName": "Pollo",
                      "brand": "Marca",
                      "format": "1 kg",
                      "imageUrl": "http://example.com/pollo.jpg",
                      "quantity": 1,
                      "unitPrice": 10,
                      "subtotal": 10
                    }
                  ]
                }
                """;
    }

    @Test
    void createListGetAndDeleteHistory() throws Exception {

        String token = registerAndGetToken(uniqueEmail("hist"));

        String created = mockMvc.perform(post("/api/history")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].productName").value("Pollo"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(10))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long historyId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(get("/api/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(historyId));

        mockMvc.perform(get("/api/history/" + historyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value("Pollo"));

        mockMvc.perform(delete("/api/history/" + historyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void otherUserCannotAccessHistory() throws Exception {

        String ownerToken = registerAndGetToken(uniqueEmail("owner"));
        String otherToken = registerAndGetToken(uniqueEmail("other"));

        String created = mockMvc.perform(post("/api/history")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyBody()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long historyId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(get("/api/history/" + historyId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/history/" + historyId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRejectsInvalidRequest() throws Exception {

        String token = registerAndGetToken(uniqueEmail("invalid"));

        mockMvc.perform(post("/api/history")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "budget": -5,
                                  "estimatedTotal": 0,
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void historyRequiresAuthentication() throws Exception {

        mockMvc.perform(get("/api/history"))
                .andExpect(status().isUnauthorized());
    }
}
