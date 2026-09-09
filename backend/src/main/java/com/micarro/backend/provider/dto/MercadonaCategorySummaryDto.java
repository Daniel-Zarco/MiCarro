package com.micarro.backend.provider.dto;

import java.util.List;

public class MercadonaCategorySummaryDto {

    private Long id;
    private String name;
    private List<MercadonaCategorySummaryDto> categories;

    public MercadonaCategorySummaryDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MercadonaCategorySummaryDto> getCategories() {
        return categories;
    }

    public void setCategories(List<MercadonaCategorySummaryDto> categories) {
        this.categories = categories;
    }
}