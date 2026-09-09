package com.micarro.backend.provider.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MercadonaProductDto {

    private String id;

    @JsonProperty("display_name")
    private String displayName;

    private String thumbnail;

    private String packaging;

    @JsonProperty("price_instructions")
    private MercadonaPriceInstructionsDto priceInstructions;

    public MercadonaProductDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public MercadonaPriceInstructionsDto getPriceInstructions() {
        return priceInstructions;
    }

    public void setPriceInstructions(MercadonaPriceInstructionsDto priceInstructions) {
        this.priceInstructions = priceInstructions;
    }
}