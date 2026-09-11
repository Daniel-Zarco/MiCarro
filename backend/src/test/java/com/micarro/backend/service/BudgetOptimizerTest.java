package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.micarro.backend.entity.Product;
import com.micarro.backend.model.ProductScore;

class BudgetOptimizerTest {

    private final BudgetOptimizer budgetOptimizer = new BudgetOptimizer();

    private ProductScore score(long id, String price, double finalScore) {

        Product product = new Product();
        product.setId(id);
        product.setName("Producto " + id);
        product.setPrice(price == null ? null : new BigDecimal(price));

        return new ProductScore(product, 1.0, 1.0, 0.0, finalScore);
    }

    @Test
    void optimize_returnsAllWhenTotalWithinBudget() {

        Map<String, ProductScore> selected = new LinkedHashMap<>();
        selected.put("pollo", score(1, "10.00", 0.9));
        selected.put("arroz", score(2, "20.00", 0.8));

        Map<String, ProductScore> result =
                budgetOptimizer.optimize(new BigDecimal("100"), selected);

        assertThat(result).hasSize(2);
        assertThat(result.keySet()).containsExactly("pollo", "arroz");
    }

    @Test
    void optimize_removesLowestFinalScoreFirstUntilWithinBudget() {

        Map<String, ProductScore> selected = new LinkedHashMap<>();
        selected.put("a", score(1, "20.00", 0.2));
        selected.put("b", score(2, "15.00", 0.5));
        selected.put("c", score(3, "10.00", 0.9));

        Map<String, ProductScore> result =
                budgetOptimizer.optimize(new BigDecimal("30"), selected);

        assertThat(result.keySet()).containsExactly("b", "c");
    }

    @Test
    void optimize_breaksTieByRemovingMostExpensiveFirst() {

        Map<String, ProductScore> selected = new LinkedHashMap<>();
        selected.put("expensive", score(1, "30.00", 0.5));
        selected.put("cheaper", score(2, "20.00", 0.5));

        Map<String, ProductScore> result =
                budgetOptimizer.optimize(new BigDecimal("25"), selected);

        assertThat(result.keySet()).containsExactly("cheaper");
    }

    @Test
    void optimize_preservesOriginalTermOrder() {

        Map<String, ProductScore> selected = new LinkedHashMap<>();
        selected.put("first", score(1, "5.00", 0.1));
        selected.put("second", score(2, "5.00", 0.9));
        selected.put("third", score(3, "5.00", 0.5));

        Map<String, ProductScore> result =
                budgetOptimizer.optimize(new BigDecimal("10"), selected);

        assertThat(result.keySet()).containsExactly("second", "third");
    }

    @Test
    void optimize_ignoresNullPricesWhenCalculatingTotal() {

        Map<String, ProductScore> selected = new LinkedHashMap<>();
        selected.put("no-price", score(1, null, 0.9));
        selected.put("priced", score(2, "5.00", 0.5));

        Map<String, ProductScore> result =
                budgetOptimizer.optimize(new BigDecimal("10"), selected);

        assertThat(result.keySet()).containsExactly("no-price", "priced");
    }

    @Test
    void optimize_keepsNullPriceProductWhenRemovingOthers() {

        Map<String, ProductScore> selected = new LinkedHashMap<>();
        selected.put("no-price", score(1, null, 0.9));
        selected.put("priced", score(2, "10.00", 0.5));

        Map<String, ProductScore> result =
                budgetOptimizer.optimize(new BigDecimal("5"), selected);

        assertThat(result.keySet()).containsExactly("no-price");
    }

    @Test
    void optimize_removesAllPricedProductsWhenBudgetIsTooLow() {

        Map<String, ProductScore> selected = new LinkedHashMap<>();
        selected.put("a", score(1, "10.00", 0.9));
        selected.put("b", score(2, "10.00", 0.8));

        Map<String, ProductScore> result =
                budgetOptimizer.optimize(new BigDecimal("5"), selected);

        assertThat(result).isEmpty();
    }

    @Test
    void optimize_returnsEmptyForEmptyOrNullInput() {

        assertThat(budgetOptimizer.optimize(
                new BigDecimal("50"), Map.of())).isEmpty();

        assertThat(budgetOptimizer.optimize(
                new BigDecimal("50"), null)).isEmpty();
    }
}
