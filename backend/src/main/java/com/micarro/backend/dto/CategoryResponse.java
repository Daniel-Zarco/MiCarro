package com.micarro.backend.dto;

public class CategoryResponse {

    private final String name;
    private final long productCount;

    public CategoryResponse(String name, long productCount) {
        this.name = name;
        this.productCount = productCount;
    }

    public String getName() {
        return name;
    }

    public long getProductCount() {
        return productCount;
    }
}