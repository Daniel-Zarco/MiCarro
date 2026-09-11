package com.micarro.backend.model;

import com.micarro.backend.entity.Product;

public class ProductScore {

    private final Product product;
    private final double relevanceScore;
    private final double priceScore;
    private final double favoriteScore;
    private final double finalScore;

    public ProductScore(
            Product product,
            double relevanceScore,
            double priceScore,
            double favoriteScore,
            double finalScore) {

        this.product = product;
        this.relevanceScore = relevanceScore;
        this.priceScore = priceScore;
        this.favoriteScore = favoriteScore;
        this.finalScore = finalScore;
    }

    public Product getProduct() {
        return product;
    }

    public double getRelevanceScore() {
        return relevanceScore;
    }

    public double getPriceScore() {
        return priceScore;
    }

    public double getFavoriteScore() {
        return favoriteScore;
    }

    public double getFinalScore() {
        return finalScore;
    }
}
