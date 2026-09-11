package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.micarro.backend.entity.Product;
import com.micarro.backend.model.ProductScore;

class BudgetMaximizerTest {

    private final BudgetMaximizer budgetMaximizer = new BudgetMaximizer();

    private ProductScore score(long id, String price) {

        Product product = new Product();
        product.setId(id);
        product.setName("Producto " + id);
        product.setPrice(price == null ? null : new BigDecimal(price));

        return new ProductScore(product, 1.0, 1.0, 0.0, 1.0);
    }

    private BigDecimal totalPrice(
            Map<String, ProductScore> selected,
            Map<String, Integer> quantities) {

        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<String, ProductScore> entry : selected.entrySet()) {

            BigDecimal price = entry.getValue().getProduct().getPrice();
            Integer quantity = quantities.get(entry.getKey());

            if (price != null && quantity != null) {
                total = total.add(price.multiply(BigDecimal.valueOf(quantity)));
            }
        }

        return total;
    }

    @Test
    void maximize_keepsQuantityOneWhenBudgetIsExhausted() {

        Map<String, ProductScore> selected = new LinkedHashMap<>();
        selected.put("pollo", score(1, "10.00"));

        Map<String, Integer> quantities =
                budgetMaximizer.maximize(new BigDecimal("10.00"), selected);

        assertThat(quantities.get("pollo")).isEqualTo(1);
    }

    @Test
    void maximize_increasesQuantitiesWithoutExceedingBudget() {

        Map<String, ProductScore> selected = new LinkedHashMap<>();
        selected.put("pollo", score(1, "6.65"));
        selected.put("arroz", score(2, "1.05"));
        selected.put("setas", score(3, "2.09"));
        selected.put("yogur", score(4, "0.90"));

        Map<String, Integer> quantities =
                budgetMaximizer.maximize(new BigDecimal("20"), selected);

        BigDecimal total = totalPrice(selected, quantities);

        assertThat(total).isLessThanOrEqualTo(new BigDecimal("20"));
        assertThat(total).isGreaterThan(new BigDecimal("10.69"));

        assertThat(quantities.values()).allMatch(quantity -> quantity >= 1);
    }

    @Test
    void maximize_neverExceedsBudget() {

        Map<String, ProductScore> selected = new LinkedHashMap<>();
        selected.put("a", score(1, "3.33"));
        selected.put("b", score(2, "2.50"));
        selected.put("c", score(3, "1.20"));

        BigDecimal budget = new BigDecimal("25.00");

        Map<String, Integer> quantities =
                budgetMaximizer.maximize(budget, selected);

        assertThat(totalPrice(selected, quantities))
                .isLessThanOrEqualTo(budget);
    }

    @Test
    void maximize_ignoresNullAndNonPositivePrices() {

        Map<String, ProductScore> selected = new LinkedHashMap<>();
        selected.put("nulo", score(1, null));
        selected.put("cero", score(2, "0.00"));
        selected.put("negativo", score(3, "-3.00"));
        selected.put("valido", score(4, "2.00"));

        Map<String, Integer> quantities =
                budgetMaximizer.maximize(new BigDecimal("100"), selected);

        assertThat(quantities.get("nulo")).isEqualTo(1);
        assertThat(quantities.get("cero")).isEqualTo(1);
        assertThat(quantities.get("negativo")).isEqualTo(1);
        assertThat(quantities.get("valido")).isGreaterThan(1);
    }

    @Test
    void maximize_distributesAcrossProducts() {

        Map<String, ProductScore> selected = new LinkedHashMap<>();
        selected.put("a", score(1, "1.00"));
        selected.put("b", score(2, "1.00"));

        Map<String, Integer> quantities =
                budgetMaximizer.maximize(new BigDecimal("10.00"), selected);

        // Baseline 2, restante 8 -> 4 incrementos por producto (5 y 5).
        assertThat(quantities.get("a")).isEqualTo(5);
        assertThat(quantities.get("b")).isEqualTo(5);
    }

    @Test
    void maximize_returnsEmptyForEmptyInput() {

        assertThat(budgetMaximizer.maximize(
                new BigDecimal("10"), Map.of())).isEmpty();

        assertThat(budgetMaximizer.maximize(
                new BigDecimal("10"), null)).isEmpty();
    }
}
