package com.micarro.backend.provider.dto;

import java.util.List;

public class MercadonaCategoryResponse {

    private Long id;
    private String name;
    private List<MercadonaSubcategoryDto> categories;

    public MercadonaCategoryResponse() {
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

    public List<MercadonaSubcategoryDto> getCategories() {
        return categories;
    }

    public void setCategories(List<MercadonaSubcategoryDto> categories) {
        this.categories = categories;
    }
}