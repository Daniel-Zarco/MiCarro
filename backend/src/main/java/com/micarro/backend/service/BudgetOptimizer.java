package com.micarro.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.micarro.backend.model.ProductScore;

@Service
public class BudgetOptimizer {

    /*
     * Orden de eliminación cuando el total supera el presupuesto:
     *   1. menor finalScore
     *   2. en caso de empate, el producto más caro
     */
    private static final Comparator<Map.Entry<String, ProductScore>> REMOVAL_ORDER =
            Comparator
                    .comparingDouble(
                            (Map.Entry<String, ProductScore> entry) ->
                                    entry.getValue().getFinalScore())
                    .thenComparing(
                            entry -> priceOf(entry.getValue()),
                            Comparator.nullsLast(Comparator.<BigDecimal>reverseOrder())
                    );

    /**
     * Ajusta la selección para que el total no supere el presupuesto.
     *
     * <p>Solo elimina productos: no ajusta cantidades ni sustituye productos
     * por alternativas. Los productos sin precio no suman al total y no se
     * eliminan para reducir coste.</p>
     */
    public Map<String, ProductScore> optimize(
            BigDecimal budget,
            Map<String, ProductScore> selectedByTerm) {

        Map<String, ProductScore> optimized = new LinkedHashMap<>();

        if (selectedByTerm == null || selectedByTerm.isEmpty()) {
            return optimized;
        }

        BigDecimal total = totalPrice(selectedByTerm.values());

        // Si ya cabe en el presupuesto, se devuelve todo tal cual.
        if (budget == null || total.compareTo(budget) <= 0) {
            optimized.putAll(selectedByTerm);
            return optimized;
        }

        List<Map.Entry<String, ProductScore>> removable =
                new ArrayList<>(selectedByTerm.entrySet());

        removable.sort(REMOVAL_ORDER);

        Set<String> removedTerms = new HashSet<>();
        BigDecimal currentTotal = total;

        for (Map.Entry<String, ProductScore> entry : removable) {

            if (currentTotal.compareTo(budget) <= 0) {
                break;
            }

            BigDecimal price = priceOf(entry.getValue());

            // Un precio null no suma al total: eliminarlo no reduce nada.
            if (price == null) {
                continue;
            }

            removedTerms.add(entry.getKey());
            currentTotal = currentTotal.subtract(price);
        }

        // Se preserva el orden original de los términos restantes.
        for (Map.Entry<String, ProductScore> entry : selectedByTerm.entrySet()) {

            if (!removedTerms.contains(entry.getKey())) {
                optimized.put(entry.getKey(), entry.getValue());
            }
        }

        return optimized;
    }

    private BigDecimal totalPrice(Collection<ProductScore> scores) {

        BigDecimal total = BigDecimal.ZERO;

        for (ProductScore score : scores) {

            BigDecimal price = priceOf(score);

            if (price != null) {
                total = total.add(price);
            }
        }

        return total;
    }

    private static BigDecimal priceOf(ProductScore score) {

        if (score == null || score.getProduct() == null) {
            return null;
        }

        return score.getProduct().getPrice();
    }
}
