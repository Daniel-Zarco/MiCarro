package com.micarro.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.micarro.backend.dto.SavedPlanNameRequest;
import com.micarro.backend.dto.SavedPlanRequest;
import com.micarro.backend.dto.SavedPlanResponse;
import com.micarro.backend.dto.SavedPlanSummaryResponse;
import com.micarro.backend.service.SavedPlanService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/plans")
public class SavedPlanController {

    private final SavedPlanService savedPlanService;

    public SavedPlanController(SavedPlanService savedPlanService) {
        this.savedPlanService = savedPlanService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavedPlanResponse create(
            @Valid @RequestBody SavedPlanRequest request,
            Authentication authentication) {

        return savedPlanService.create(authentication.getName(), request);
    }

    @GetMapping
    public List<SavedPlanSummaryResponse> getPlans(
            Authentication authentication) {

        return savedPlanService.getPlans(authentication.getName());
    }

    @GetMapping("/{id}")
    public SavedPlanResponse getPlanById(
            @PathVariable Long id,
            Authentication authentication) {

        return savedPlanService.getPlanById(authentication.getName(), id);
    }

    @PatchMapping("/{id}/name")
    public SavedPlanResponse rename(
            @PathVariable Long id,
            @Valid @RequestBody SavedPlanNameRequest request,
            Authentication authentication) {

        return savedPlanService.rename(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication authentication) {

        savedPlanService.delete(authentication.getName(), id);
    }
}