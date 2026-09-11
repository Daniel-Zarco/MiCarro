package com.micarro.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.dev-endpoints-enabled=false")
@AutoConfigureMockMvc
class DevEndpointsDisabledTest {

    private static final String PLAN_BODY = """
            {
              "budget": 60,
              "items": ["pollo"],
              "mode": "BALANCED",
              "preferences": { "prioritizeFavorites": false, "maximizeBudget": false }
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void candidatesEndpointIsNotExposedWhenDisabled() throws Exception {

        mockMvc.perform(post("/api/shopping-plans/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLAN_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void scoresEndpointIsNotExposedWhenDisabled() throws Exception {

        mockMvc.perform(post("/api/shopping-plans/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLAN_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void mercadonaEndpointsAreNotExposedWhenDisabled() throws Exception {

        mockMvc.perform(get("/api/mercadona/categories"))
                .andExpect(status().isNotFound());
    }

    @Test
    void normalPublicEndpointsStillWork() throws Exception {

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/shopping-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLAN_BODY))
                .andExpect(status().isOk());
    }
}
