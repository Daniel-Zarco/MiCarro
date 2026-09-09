package com.micarro.backend.provider.dto;

import java.util.List;

public class MercadonaCategoriesResponse {

    private List<MercadonaCategorySummaryDto> results;

    public MercadonaCategoriesResponse() {
    }

    public List<MercadonaCategorySummaryDto> getResults() {
        return results;
    }

    public void setResults(List<MercadonaCategorySummaryDto> results) {
        this.results = results;
    }
}