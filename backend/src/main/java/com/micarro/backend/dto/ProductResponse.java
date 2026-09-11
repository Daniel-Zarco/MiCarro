package com.micarro.backend.dto;

import java.math.BigDecimal;

public class ProductResponse {

    private final Long id;
    private final String externalId;
    private final String name;
    private final String brand;
    private final String category;
    private final String imageUrl;
    private final String format;
    private final BigDecimal price;

    public ProductResponse(
            Long id,
            String externalId,
            String name,
            String brand,
            String category,
            String imageUrl,
            String format,
            BigDecimal price) {

        this.id = id;
        this.externalId = externalId;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.imageUrl = imageUrl;
        this.format = format;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getFormat() {
        return format;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
