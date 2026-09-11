package com.micarro.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorHealthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsExposedWithoutAuthentication() throws Exception {

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void onlyHealthEndpointIsExposed() throws Exception {

        List<String> hidden = List.of(
                "/actuator/env",
                "/actuator/beans",
                "/actuator/mappings",
                "/actuator/metrics",
                "/actuator/configprops",
                "/actuator/loggers"
        );

        for (String path : hidden) {
            mockMvc.perform(get(path))
                    .andExpect(status().isNotFound());
        }
    }
}
