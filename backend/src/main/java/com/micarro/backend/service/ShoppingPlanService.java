package com.micarro.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final BudgetMaximizer budgetMaximizer;
    private final FavoriteService favoriteService;

    public ShoppingPlanService(
            CandidateSelectionService candidateSelectionService,
            ScoringEngineService scoringEngineService,
            ProductSelectionService productSelectionService,
            BudgetOptimizer budgetOptimizer,
            BudgetMaximizer budgetMaximizer,
            FavoriteService favoriteService) {

        this.candidateSelectionService = candidateSelectionService;
        this.scoringEngineService = scoringEngineService;
        this.productSelectionService = productSelectionService;
        this.budgetOptimizer = budgetOptimizer;
        this.budgetMaximizer = budgetMaximizer;
        this.favoriteService = favoriteService;
    }

    public ShoppingPlanResponse plan(
            ShoppingPlanRequest request,
            String userEmail) {

        Set<Long> favoriteProductIds = resolveFavoriteProductIds(request, userEmail);

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
                            request,
                            favoriteProductIds
                    )
            );
        }

        // 3. Mejor producto por término.
        Map<String, ProductScore> winners =
                productSelectionService.selectBestPerTerm(scoresByTerm);

        // 3b. Ajuste al presupuesto (solo elimina, no sustituye).
        Map<String, ProductScore> optimizedWinners =
                budgetOptimizer.optimize(request.getBudget(), winners);

        // 3c. Maximización opcional del presupuesto restante.
        Map<String, Integer> quantities =
                resolveQuantities(request, optimizedWinners);

        // 4 + 5. Conversión a items.
        List<ShoppingPlanItemResponse> items = new ArrayList<>();
        BigDecimal estimatedTotal = BigDecimal.ZERO;

        for (Map.Entry<String, ProductScore> entry : optimizedWinners.entrySet()) {

            int quantity = quantities.getOrDefault(entry.getKey(), 1);

            ShoppingPlanItemResponse item = toItem(
                    entry.getValue().getProduct(),
                    quantity
            );

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

    /**
     * Devuelve los ids de favoritos que deben puntuar. Solo aplica si hay
     * usuario autenticado y la preferencia {@code prioritizeFavorites} está
     * activada; en cualquier otro caso devuelve un conjunto vacío.
     */
    public Set<Long> resolveFavoriteProductIds(
            ShoppingPlanRequest request,
            String userEmail) {

        if (userEmail == null
                || request == null
                || request.getPreferences() == null
                || !request.getPreferences().isPrioritizeFavorites()) {
            return Set.of();
        }

        return favoriteService.getFavorites(userEmail)
                .stream()
                .map(Product::getId)
                .collect(Collectors.toSet());
    }

    private Map<String, Integer> resolveQuantities(
            ShoppingPlanRequest request,
            Map<String, ProductScore> selectedByTerm) {

        if (request.getPreferences() != null
                && request.getPreferences().isMaximizeBudget()) {

            return budgetMaximizer.maximize(
                    request.getBudget(),
                    selectedByTerm
            );
        }

        Map<String, Integer> quantities = new LinkedHashMap<>();

        for (String term : selectedByTerm.keySet()) {
            quantities.put(term, 1);
        }

        return quantities;
    }

    private ShoppingPlanItemResponse toItem(Product product, int quantity) {

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
