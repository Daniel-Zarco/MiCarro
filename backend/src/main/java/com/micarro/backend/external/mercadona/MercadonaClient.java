package com.micarro.backend.external.mercadona;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.micarro.backend.provider.dto.MercadonaCategoryResponse;
import com.micarro.backend.provider.dto.MercadonaCategorySummaryDto;
import com.micarro.backend.provider.dto.MercadonaCategoriesResponse;

@Component
public class MercadonaClient {

    private final RestClient restClient;

    public MercadonaClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://tienda.mercadona.es/api")
                .build();
    }

    public MercadonaCategoryResponse getCategory(Long categoryId) {

        return restClient.get()
                .uri("/categories/{id}/", categoryId)
                .retrieve()
                .body(MercadonaCategoryResponse.class);
    }

    public MercadonaCategoriesResponse getCategories() {
        
        return restClient.get()
                .uri("/categories/")
                .retrieve()
                .body(MercadonaCategoriesResponse.class);
    }
}