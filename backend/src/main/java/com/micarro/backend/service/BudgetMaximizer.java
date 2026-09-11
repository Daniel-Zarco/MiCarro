package com.micarro.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.micarro.backend.model.ProductScore;

@Service
public class BudgetMaximizer {

    /**
     * Reparte el presupuesto restante aumentando cantidades de los productos ya
     * seleccionados, de forma equilibrada y sin superar nunca el presupuesto.
     *
     * <p>No añade productos nuevos ni cambia el producto ganador. Los productos
     * con precio nulo o &lt;= 0 se ignoran. La cantidad mínima es 1.</p>
     *
     * @return cantidad final por término (mismo orden que la selección).
     */
    public Map<String, Integer> maximize(
            BigDecimal budget,
            Map<String, ProductScore> selectedByTerm) {

        Map<String, Integer> quantities = new LinkedHashMap<>();

        if (selectedByTerm == null || selectedByTerm.isEmpty()) {
            return quantities;
        }

        List<Map.Entry<String, ProductScore>> affordable = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<String, ProductScore> entry : selectedByTerm.entrySet()) {

            quantities.put(entry.getKey(), 1);

            BigDecimal price = validPrice(entry.getValue());

            if (price != null) {
                total = total.add(price);
                affordable.add(entry);
            }
        }

        if (budget == null || affordable.isEmpty()) {
            return quantities;
        }

        BigDecimal remaining = budget.subtract(total);

        if (remaining.signum() <= 0) {
            return quantities;
        }

        // Reparto equilibrado: una unidad por producto en cada pasada.
        boolean advanced = true;

        while (advanced) {

            advanced = false;

            for (Map.Entry<String, ProductScore> entry : affordable) {

                BigDecimal price = validPrice(entry.getValue());

                if (price != null && price.compareTo(remaining) <= 0) {
                    quantities.merge(entry.getKey(), 1, Integer::sum);
                    remaining = remaining.subtract(price);
                    advanced = true;
                }
            }
        }

        return quantities;
    }

    private BigDecimal validPrice(ProductScore score) {

        if (score == null || score.getProduct() == null) {
            return null;
        }

        BigDecimal price = score.getProduct().getPrice();

        if (price == null || price.signum() <= 0) {
            return null;
        }

        return price;
    }
}
