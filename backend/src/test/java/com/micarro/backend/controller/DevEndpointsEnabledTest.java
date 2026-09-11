package com.micarro.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DevEndpointsEnabledTest {

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
    void candidatesEndpointIsAvailableWhenEnabled() throws Exception {

        mockMvc.perform(post("/api/shopping-plans/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLAN_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void scoresEndpointIsAvailableWhenEnabled() throws Exception {

        mockMvc.perform(post("/api/shopping-plans/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLAN_BODY))
                .andExpect(status().isOk());
    }
}
