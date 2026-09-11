package com.micarro.backend.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.micarro.backend.dto.ShoppingPlanItemResponse;
import com.micarro.backend.dto.ShoppingPlanRequest;
import com.micarro.backend.dto.ShoppingPlanResponse;

@Service
public class ShoppingPlanService {

    public ShoppingPlanResponse plan(ShoppingPlanRequest request) {

        // Fase 3: la petición ya viene validada y aquí solo se prepara
        // la estructura de respuesta.
        //
        // La selección de productos, el scoring y la optimización del
        // presupuesto se implementarán en fases posteriores. Por eso
        // todavía no se devuelve ninguna cesta.

        List<ShoppingPlanItemResponse> items = List.of();

        ShoppingPlanResponse response = new ShoppingPlanResponse();

        response.setBudget(request.getBudget());
        response.setMode(request.getMode());
        response.setPreferences(request.getPreferences());
        response.setItems(items);
        response.setEstimatedTotal(BigDecimal.ZERO);
        response.setRemainingBudget(request.getBudget());

        return response;
    }
}
