package com.micarro.backend.provider.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MercadonaPriceInstructionsDto {

    @JsonProperty("unit_price")
    private String unitPrice;

    @JsonProperty("bulk_price")
    private String bulkPrice;

    @JsonProperty("unit_size")
    private Double unitSize;

    @JsonProperty("size_format")
    private String sizeFormat;

    public MercadonaPriceInstructionsDto() {
    }

    public String getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(String unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getBulkPrice() {
        return bulkPrice;
    }

    public void setBulkPrice(String bulkPrice) {
        this.bulkPrice = bulkPrice;
    }

    public Double getUnitSize() {
        return unitSize;
    }

    public void setUnitSize(Double unitSize) {
        this.unitSize = unitSize;
    }

    public String getSizeFormat() {
        return sizeFormat;
    }

    public void setSizeFormat(String sizeFormat) {
        this.sizeFormat = sizeFormat;
    }
}