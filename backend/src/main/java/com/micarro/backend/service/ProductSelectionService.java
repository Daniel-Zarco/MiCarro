package com.micarro.backend.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.micarro.backend.model.ProductScore;

@Service
public class ProductSelectionService {

    /*
     * Filtro semántico previo: solo se consideran candidatos cuya relevancia
     * esté a menos de RELEVANCE_TOLERANCE del máximo. Esto evita que un
     * producto mucho más barato pero claramente menos relevante gane solo por
     * su puntuación de precio.
     */
    private static final double RELEVANCE_TOLERANCE = 0.15;

    private static final double EPSILON = 1e-9;

    /*
     * Criterio de "mejor" producto (sobre los candidatos que pasan el filtro):
     *   1. mayor finalScore
     *   2. mayor relevanceScore
     *   3. mayor priceScore
     *   4. menor product.id (resultado determinista)
     */
    private static final Comparator<ProductScore> BEST_FIRST =
            Comparator
                    .comparingDouble(ProductScore::getFinalScore)
                    .thenComparingDouble(ProductScore::getRelevanceScore)
                    .thenComparingDouble(ProductScore::getPriceScore)
                    .thenComparing(
                            score -> score.getProduct() == null
                                    ? null
                                    : score.getProduct().getId(),
                            Comparator.nullsFirst(Comparator.<Long>reverseOrder())
                    );

    /**
     * Devuelve el mejor candidato para un término.
     *
     * <p>Primero descarta los candidatos claramente menos relevantes que el
     * máximo y, entre los que quedan, aplica el criterio actual. No aplica
     * presupuesto ni cantidades.</p>
     */
    public Optional<ProductScore> selectBest(
            String term,
            List<ProductScore> scores) {

        if (scores == null || scores.isEmpty()) {
            return Optional.empty();
        }

        double maxRelevance = scores.stream()
                .mapToDouble(ProductScore::getRelevanceScore)
                .max()
                .orElse(0.0);

        double threshold =
                maxRelevance - RELEVANCE_TOLERANCE - EPSILON;

        return scores.stream()
                .filter(score -> score.getRelevanceScore() >= threshold)
                .max(BEST_FIRST);
    }

    /**
     * Elige un ganador para cada término. Los términos sin candidatos se
     * omiten del resultado.
     */
    public Map<String, ProductScore> selectBestPerTerm(
            Map<String, List<ProductScore>> scoresByTerm) {

        Map<String, ProductScore> winners = new LinkedHashMap<>();

        if (scoresByTerm == null || scoresByTerm.isEmpty()) {
            return winners;
        }

        for (Map.Entry<String, List<ProductScore>> entry : scoresByTerm.entrySet()) {

            selectBest(entry.getKey(), entry.getValue())
                    .ifPresent(winner -> winners.put(entry.getKey(), winner));
        }

        return winners;
    }
}
