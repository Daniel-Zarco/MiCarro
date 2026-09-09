package com.micarro.backend.provider.dto;

import java.util.List;

public class MercadonaSubcategoryDto {

    private Long id;
    private String name;
    private List<MercadonaProductDto> products;

    public MercadonaSubcategoryDto() {
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

    public List<MercadonaProductDto> getProducts() {
        return products;
    }

    public void setProducts(List<MercadonaProductDto> products) {
        this.products = products;
    }
}