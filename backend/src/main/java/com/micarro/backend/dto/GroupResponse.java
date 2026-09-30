package com.micarro.backend.dto;

public class GroupResponse {

    private final String name;
    private final long productCount;

    public GroupResponse(String name, long productCount) {
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