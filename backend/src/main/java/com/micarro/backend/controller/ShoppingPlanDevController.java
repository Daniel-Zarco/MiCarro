package com.micarro.backend.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.micarro.backend.dto.ShoppingPlanRequest;
import com.micarro.backend.entity.Product;
import com.micarro.backend.model.ProductScore;
import com.micarro.backend.service.CandidateSelectionService;
import com.micarro.backend.service.ScoringEngineService;
import com.micarro.backend.service.ShoppingPlanService;

import jakarta.validation.Valid;

// =========================================================
// ENDPOINTS TEMPORALES DE DESARROLLO
//
// Permiten inspeccionar candidatos y scoring sin afectar al
// flujo real de POST /api/shopping-plans. No aplican selección
// final ni optimización de presupuesto.
//
// Se habilitan solo con app.dev-endpoints-enabled=true (local).
// En producción deben quedar deshabilitados (app.dev-endpoints-enabled=false),
// de modo que estos endpoints ni siquiera se registren.
// =========================================================
@RestController
@RequestMapping("/api/shopping-plans")
@ConditionalOnProperty(
        name = "app.dev-endpoints-enabled",
        havingValue = "true"
)
public class ShoppingPlanDevController {

    private final CandidateSelectionService candidateSelectionService;
    private final ScoringEngineService scoringEngineService;
    private final ShoppingPlanService shoppingPlanService;

    public ShoppingPlanDevController(
            CandidateSelectionService candidateSelectionService,
            ScoringEngineService scoringEngineService,
            ShoppingPlanService shoppingPlanService) {

        this.candidateSelectionService = candidateSelectionService;
        this.scoringEngineService = scoringEngineService;
        this.shoppingPlanService = shoppingPlanService;
    }

    @PostMapping("/candidates")
    public Map<String, List<Product>> getCandidates(
            @Valid @RequestBody ShoppingPlanRequest request) {

        return candidateSelectionService.selectCandidates(request);
    }

    @PostMapping("/scores")
    public Map<String, List<ProductScore>> getScores(
            @Valid @RequestBody ShoppingPlanRequest request,
            Authentication authentication) {

        Map<String, List<Product>> candidatesByItem =
                candidateSelectionService.selectCandidates(request);

        Set<Long> favoriteProductIds =
                shoppingPlanService.resolveFavoriteProductIds(
                        request,
                        resolveEmail(authentication)
                );

        Map<String, List<ProductScore>> scoresByItem = new LinkedHashMap<>();

        for (Map.Entry<String, List<Product>> entry : candidatesByItem.entrySet()) {

            scoresByItem.put(
                    entry.getKey(),
                    scoringEngineService.scoreCandidates(
                            entry.getKey(),
                            entry.getValue(),
                            request,
                            favoriteProductIds
                    )
            );
        }

        return scoresByItem;
    }

    private String resolveEmail(Authentication authentication) {

        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            return authentication.getName();
        }

        return null;
    }
}
