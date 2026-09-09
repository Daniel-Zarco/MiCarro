package com.micarro.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.micarro.backend.entity.Product;
import com.micarro.backend.external.mercadona.MercadonaClient;
import com.micarro.backend.provider.dto.MercadonaCategoryResponse;
import com.micarro.backend.service.ProductImportService;
import com.micarro.backend.provider.dto.MercadonaCategoriesResponse;

@RestController
@RequestMapping("/api/mercadona")
public class MercadonaController {

    private final MercadonaClient mercadonaClient;
    private final ProductImportService productImportService;

    public MercadonaController(
            MercadonaClient mercadonaClient,
            ProductImportService productImportService) {

        this.mercadonaClient = mercadonaClient;
        this.productImportService = productImportService;
    }

    @GetMapping("/categories/{id}")
    public MercadonaCategoryResponse getCategory(@PathVariable Long id) {
        return mercadonaClient.getCategory(id);
    }

    @PostMapping("/import")
    public List<Product> importProducts() {
        return productImportService.importProducts();
    }

    @GetMapping("/categories")
    public MercadonaCategoriesResponse getCategories() {
        return mercadonaClient.getCategories();
    }
}