package com.micarro.backend.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.micarro.backend.dto.ShoppingPlanRequest;
import com.micarro.backend.dto.ShoppingPlanResponse;
import com.micarro.backend.entity.Product;
import com.micarro.backend.model.ProductScore;
import com.micarro.backend.service.CandidateSelectionService;
import com.micarro.backend.service.ScoringEngineService;
import com.micarro.backend.service.ShoppingPlanService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/shopping-plans")
public class ShoppingPlanController {

    private final ShoppingPlanService shoppingPlanService;
    private final CandidateSelectionService candidateSelectionService;
    private final ScoringEngineService scoringEngineService;

    public ShoppingPlanController(
            ShoppingPlanService shoppingPlanService,
            CandidateSelectionService candidateSelectionService,
            ScoringEngineService scoringEngineService) {

        this.shoppingPlanService = shoppingPlanService;
        this.candidateSelectionService = candidateSelectionService;
        this.scoringEngineService = scoringEngineService;
    }

    @PostMapping
    public ShoppingPlanResponse createPlan(@Valid @RequestBody ShoppingPlanRequest request) {
        return shoppingPlanService.plan(request);
    }

    // =========================================================
    // ENDPOINT TEMPORAL DE DESARROLLO
    //
    // Permite probar CandidateSelectionService sin tocar el
    // flujo real de POST /api/shopping-plans. No aplica scoring,
    // selección final ni optimización de presupuesto.
    //
    // Eliminar (o mover a un perfil de desarrollo) cuando el
    // planificador completo esté implementado.
    // =========================================================
    @PostMapping("/candidates")
    public Map<String, List<Product>> getCandidates(
            @Valid @RequestBody ShoppingPlanRequest request) {

        return candidateSelectionService.selectCandidates(request);
    }

    // =========================================================
    // ENDPOINT TEMPORAL DE DESARROLLO
    //
    // Permite inspeccionar el scoring real de los candidatos.
    // Mantiene el orden actual de los candidatos y todavía NO
    // elige ganador ni optimiza el presupuesto.
    //
    // Eliminar (o mover a un perfil de desarrollo) cuando el
    // planificador completo esté implementado.
    // =========================================================
    @PostMapping("/scores")
    public Map<String, List<ProductScore>> getScores(
            @Valid @RequestBody ShoppingPlanRequest request) {

        Map<String, List<Product>> candidatesByItem =
                candidateSelectionService.selectCandidates(request);

        Map<String, List<ProductScore>> scoresByItem = new LinkedHashMap<>();

        for (Map.Entry<String, List<Product>> entry : candidatesByItem.entrySet()) {

            scoresByItem.put(
                    entry.getKey(),
                    scoringEngineService.scoreCandidates(
                            entry.getKey(),
                            entry.getValue(),
                            request
                    )
            );
        }

        return scoresByItem;
    }
}
