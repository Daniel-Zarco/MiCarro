package com.micarro.backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.micarro.backend.dto.ShoppingPlanRequest;
import com.micarro.backend.dto.ShoppingPlanResponse;
import com.micarro.backend.service.ShoppingPlanService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/shopping-plans")
public class ShoppingPlanController {

    private final ShoppingPlanService shoppingPlanService;

    public ShoppingPlanController(ShoppingPlanService shoppingPlanService) {
        this.shoppingPlanService = shoppingPlanService;
    }

    @PostMapping
    public ShoppingPlanResponse createPlan(@Valid @RequestBody ShoppingPlanRequest request) {
        return shoppingPlanService.plan(request);
    }
}
