package com.micarro.backend.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import com.micarro.backend.entity.Product;
import com.micarro.backend.repository.ProductRepository.ProductPriceChangeRow;

/*
 * Producto con su cambio de precio frente a hace la ventana configurada:
 * precio anterior, actual, diferencia en € y en %. Para "Nuevos" los campos de
 * cambio van a null (solo se rellena firstSeenAt).
 */
public class ProductPriceChangeResponse {

    private final Long id;
    private final String externalId;
    private final String name;
    private final String brand;
    private final String category;
    private final String imageUrl;
    private final String format;
    private final BigDecimal currentPrice;
    private final BigDecimal previousPrice;
    private final BigDecimal difference;
    private final BigDecimal differencePercent;
    private final Instant firstSeenAt;

    public ProductPriceChangeResponse(
            Long id,
            String externalId,
            String name,
            String brand,
            String category,
            String imageUrl,
            String format,
            BigDecimal currentPrice,
            BigDecimal previousPrice,
            BigDecimal difference,
            BigDecimal differencePercent,
            Instant firstSeenAt) {

        this.id = id;
        this.externalId = externalId;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.imageUrl = imageUrl;
        this.format = format;
        this.currentPrice = currentPrice;
        this.previousPrice = previousPrice;
        this.difference = difference;
        this.differencePercent = differencePercent;
        this.firstSeenAt = firstSeenAt;
    }

    public static ProductPriceChangeResponse fromRow(ProductPriceChangeRow row) {

        BigDecimal current = row.getCurrentPrice();
        BigDecimal previous = row.getPreviousPrice();

        BigDecimal difference = null;
        BigDecimal percent = null;

        if (current != null && previous != null) {
            difference = current.subtract(previous);

            if (previous.compareTo(BigDecimal.ZERO) != 0) {
                percent = difference
                        .divide(previous, 10, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        return new ProductPriceChangeResponse(
                row.getId(),
                row.getExternalId(),
                row.getName(),
                row.getBrand(),
                row.getCategory(),
                row.getImageUrl(),
                row.getFormat(),
                current,
                previous,
                difference,
                percent,
                null
        );
    }

    public static ProductPriceChangeResponse forNewProduct(Product product) {

        return new ProductPriceChangeResponse(
                product.getId(),
                product.getExternalId(),
                product.getName(),
                product.getBrand(),
                product.getCategory(),
                product.getImageUrl(),
                product.getFormat(),
                product.getPrice(),
                null,
                null,
                null,
                product.getFirstSeenAt()
        );
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

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getPreviousPrice() {
        return previousPrice;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public BigDecimal getDifferencePercent() {
        return differencePercent;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }
}