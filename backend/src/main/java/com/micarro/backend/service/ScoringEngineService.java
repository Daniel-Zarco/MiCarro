package com.micarro.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.micarro.backend.dto.ShoppingPlanRequest;
import com.micarro.backend.entity.Product;
import com.micarro.backend.model.ProductScore;
import com.micarro.backend.model.ShoppingMode;
import com.micarro.backend.strategy.ShoppingStrategy;

@Service
public class ScoringEngineService {

    /*
     * Relevancia según el tipo de coincidencia con el término buscado.
     * Valores más altos = coincidencia más significativa.
     * Se aplica siempre la coincidencia más alta que cumpla el producto.
     */
    static final double RELEVANCE_EXACT = 1.0;
    static final double RELEVANCE_PREFIX_AND_CATEGORY = 0.95;
    static final double RELEVANCE_CATEGORY = 0.85;
    static final double RELEVANCE_PREFIX = 0.70;
    static final double RELEVANCE_CONTAINS = 0.50;
    static final double RELEVANCE_FALLBACK = 0.30;
    static final double RELEVANCE_NONE = 0.0;

    private static final ShoppingMode DEFAULT_MODE = ShoppingMode.BALANCED;

    private final Map<ShoppingMode, ShoppingStrategy> strategies;
    private final ShoppingStrategy defaultStrategy;

    public ScoringEngineService(List<ShoppingStrategy> strategies) {

        Map<ShoppingMode, ShoppingStrategy> byMode =
                new EnumMap<>(ShoppingMode.class);

        for (ShoppingStrategy strategy : strategies) {
            byMode.put(strategy.mode(), strategy);
        }

        this.strategies = byMode;
        this.defaultStrategy = byMode.get(DEFAULT_MODE);
    }

    /**
     * Puntúa cada candidato para el término indicado.
     *
     * <p>No selecciona ganador ni optimiza presupuesto: solo calcula las
     * puntuaciones. Los resultados se devuelven en el mismo orden que los
     * candidatos recibidos.</p>
     */
    public List<ProductScore> scoreCandidates(
            String term,
            List<Product> candidates,
            ShoppingPlanRequest request) {

        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        ShoppingStrategy strategy = strategyFor(request);

        BigDecimal minPrice = minPrice(candidates);
        BigDecimal maxPrice = maxPrice(candidates);

        List<ProductScore> scores = new ArrayList<>(candidates.size());

        for (Product candidate : candidates) {

            double relevanceScore = relevanceScore(term, candidate);
            double priceScore = priceScore(candidate, minPrice, maxPrice);
            double favoriteScore = favoriteScore(candidate, request);

            double finalScore =
                    relevanceScore * strategy.relevanceWeight()
                            + priceScore * strategy.priceWeight();

            scores.add(new ProductScore(
                    candidate,
                    relevanceScore,
                    priceScore,
                    favoriteScore,
                    finalScore
            ));
        }

        return scores;
    }

    private ShoppingStrategy strategyFor(ShoppingPlanRequest request) {

        if (request == null || request.getMode() == null) {
            return defaultStrategy;
        }

        ShoppingStrategy strategy = strategies.get(request.getMode());

        return strategy != null ? strategy : defaultStrategy;
    }

    private double relevanceScore(String term, Product product) {

        if (term == null || term.isBlank()) {
            return RELEVANCE_NONE;
        }

        String normalizedTerm = term.trim().toLowerCase(Locale.ROOT);

        String name = normalize(product.getName());
        String category = normalize(product.getCategory());
        String brand = normalize(product.getBrand());

        boolean prefixMatch = name.startsWith(normalizedTerm);
        boolean categoryMatch = category.contains(normalizedTerm);

        if (name.equals(normalizedTerm)) {
            return RELEVANCE_EXACT;
        }

        if (prefixMatch && categoryMatch) {
            return RELEVANCE_PREFIX_AND_CATEGORY;
        }

        if (categoryMatch) {
            return RELEVANCE_CATEGORY;
        }

        if (prefixMatch) {
            return RELEVANCE_PREFIX;
        }

        if (name.contains(normalizedTerm)) {
            return RELEVANCE_CONTAINS;
        }

        if (brand.contains(normalizedTerm)) {
            return RELEVANCE_FALLBACK;
        }

        return RELEVANCE_NONE;
    }

    private double priceScore(
            Product product,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        BigDecimal price = product.getPrice();

        if (price == null || minPrice == null || maxPrice == null) {
            return 0.0;
        }

        // Todos los precios iguales: todos obtienen la mejor puntuación.
        if (maxPrice.compareTo(minPrice) == 0) {
            return 1.0;
        }

        // Menor precio => mayor puntuación, relativo al rango de candidatos.
        double range = maxPrice.subtract(minPrice).doubleValue();
        double distanceFromMin = price.subtract(minPrice).doubleValue();

        return 1.0 - (distanceFromMin / range);
    }

    /*
     * Aún no hay usuarios ni favoritos en backend, por lo que la puntuación
     * de favoritos queda preparada pero siempre a 0.
     */
    private double favoriteScore(Product product, ShoppingPlanRequest request) {
        return 0.0;
    }

    private BigDecimal minPrice(List<Product> candidates) {

        BigDecimal min = null;

        for (Product product : candidates) {

            BigDecimal price = product.getPrice();

            if (price != null && (min == null || price.compareTo(min) < 0)) {
                min = price;
            }
        }

        return min;
    }

    private BigDecimal maxPrice(List<Product> candidates) {

        BigDecimal max = null;

        for (Product product : candidates) {

            BigDecimal price = product.getPrice();

            if (price != null && (max == null || price.compareTo(max) > 0)) {
                max = price;
            }
        }

        return max;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
