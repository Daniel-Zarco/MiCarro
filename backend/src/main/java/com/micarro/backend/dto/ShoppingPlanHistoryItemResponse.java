package com.micarro.backend.dto;

import java.math.BigDecimal;

public class ShoppingPlanHistoryItemResponse {

    private final Long productId;
    private final String productName;
    private final String brand;
    private final String format;
    private final String imageUrl;
    private final Integer quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;

    public ShoppingPlanHistoryItemResponse(
            Long productId,
            String productName,
            String brand,
            String format,
            String imageUrl,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal) {

        this.productId = productId;
        this.productName = productName;
        this.brand = brand;
        this.format = format;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getBrand() {
        return brand;
    }

    public String getFormat() {
        return format;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
