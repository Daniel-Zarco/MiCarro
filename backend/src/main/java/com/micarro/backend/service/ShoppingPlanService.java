package com.micarro.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.micarro.backend.dto.ShoppingPlanItemResponse;
import com.micarro.backend.dto.ShoppingPlanRequest;
import com.micarro.backend.dto.ShoppingPlanResponse;
import com.micarro.backend.entity.Product;
import com.micarro.backend.model.ProductScore;

@Service
public class ShoppingPlanService {

    private final CandidateSelectionService candidateSelectionService;
    private final ScoringEngineService scoringEngineService;
    private final ProductSelectionService productSelectionService;
    private final BudgetOptimizer budgetOptimizer;

    public ShoppingPlanService(
            CandidateSelectionService candidateSelectionService,
            ScoringEngineService scoringEngineService,
            ProductSelectionService productSelectionService,
            BudgetOptimizer budgetOptimizer) {

        this.candidateSelectionService = candidateSelectionService;
        this.scoringEngineService = scoringEngineService;
        this.productSelectionService = productSelectionService;
        this.budgetOptimizer = budgetOptimizer;
    }

    public ShoppingPlanResponse plan(ShoppingPlanRequest request) {

        // 1 + 2. Candidatos por término y scoring por término.
        Map<String, List<Product>> candidatesByTerm =
                candidateSelectionService.selectCandidates(request);

        Map<String, List<ProductScore>> scoresByTerm = new LinkedHashMap<>();

        for (Map.Entry<String, List<Product>> entry : candidatesByTerm.entrySet()) {

            scoresByTerm.put(
                    entry.getKey(),
                    scoringEngineService.scoreCandidates(
                            entry.getKey(),
                            entry.getValue(),
                            request
                    )
            );
        }

        // 3. Mejor producto por término.
        Map<String, ProductScore> winners =
                productSelectionService.selectBestPerTerm(scoresByTerm);

        // 3b. Ajuste al presupuesto (solo elimina, no sustituye).
        Map<String, ProductScore> optimizedWinners =
                budgetOptimizer.optimize(request.getBudget(), winners);

        // 4 + 5. Conversión a items (quantity = 1, subtotal = unitPrice).
        List<ShoppingPlanItemResponse> items = new ArrayList<>();
        BigDecimal estimatedTotal = BigDecimal.ZERO;

        for (ProductScore winner : optimizedWinners.values()) {

            ShoppingPlanItemResponse item = toItem(winner.getProduct());

            items.add(item);

            if (item.getSubtotal() != null) {
                estimatedTotal = estimatedTotal.add(item.getSubtotal());
            }
        }

        // 6 + 7. Totales.
        ShoppingPlanResponse response = new ShoppingPlanResponse();

        response.setBudget(request.getBudget());
        response.setMode(request.getMode());
        response.setPreferences(request.getPreferences());
        response.setItems(items);
        response.setEstimatedTotal(estimatedTotal);
        response.setRemainingBudget(
                request.getBudget().subtract(estimatedTotal)
        );

        return response;
    }

    private ShoppingPlanItemResponse toItem(Product product) {

        int quantity = 1;

        BigDecimal unitPrice = product.getPrice();

        BigDecimal subtotal = unitPrice == null
                ? null
                : unitPrice.multiply(BigDecimal.valueOf(quantity));

        ShoppingPlanItemResponse item = new ShoppingPlanItemResponse();

        item.setProductId(product.getId());
        item.setName(product.getName());
        item.setBrand(product.getBrand());
        item.setFormat(product.getFormat());
        item.setImageUrl(product.getImageUrl());
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setSubtotal(subtotal);

        return item;
    }
}
