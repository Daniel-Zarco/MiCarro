package com.micarro.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.micarro.backend.dto.ShoppingPlanHistoryRequest;
import com.micarro.backend.dto.ShoppingPlanHistoryResponse;
import com.micarro.backend.dto.ShoppingPlanHistorySummaryResponse;
import com.micarro.backend.service.ShoppingPlanHistoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/history")
public class ShoppingPlanHistoryController {

    private final ShoppingPlanHistoryService historyService;

    public ShoppingPlanHistoryController(
            ShoppingPlanHistoryService historyService) {

        this.historyService = historyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShoppingPlanHistoryResponse create(
            @Valid @RequestBody ShoppingPlanHistoryRequest request,
            Authentication authentication) {

        return historyService.create(authentication.getName(), request);
    }

    @GetMapping
    public List<ShoppingPlanHistorySummaryResponse> getHistory(
            Authentication authentication) {

        return historyService.getHistory(authentication.getName());
    }

    @GetMapping("/{id}")
    public ShoppingPlanHistoryResponse getById(
            @PathVariable Long id,
            Authentication authentication) {

        return historyService.getById(authentication.getName(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication authentication) {

        historyService.delete(authentication.getName(), id);
    }
}
