package com.micarro.backend.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.micarro.backend.dto.SyncResult;
import com.micarro.backend.external.mercadona.MercadonaClient;
import com.micarro.backend.provider.dto.MercadonaCategoryResponse;
import com.micarro.backend.service.ProductSyncService;
import com.micarro.backend.provider.dto.MercadonaCategoriesResponse;

/*
 * Endpoints internos de importación/catálogo de Mercadona.
 * Solo se registran con app.dev-endpoints-enabled=true (local), para que
 * en producción no queden expuestos.
 */
@RestController
@RequestMapping("/api/mercadona")
@ConditionalOnProperty(
        name = "app.dev-endpoints-enabled",
        havingValue = "true"
)
public class MercadonaController {

    private final MercadonaClient mercadonaClient;
    private final ProductSyncService productSyncService;

    public MercadonaController(
            MercadonaClient mercadonaClient,
            ProductSyncService productSyncService) {

        this.mercadonaClient = mercadonaClient;
        this.productSyncService = productSyncService;
    }

    @GetMapping("/categories/{id}")
    public MercadonaCategoryResponse getCategory(@PathVariable Long id) {
        return mercadonaClient.getCategory(id);
    }

    @PostMapping("/import")
    public SyncResult importProducts() {
        return productSyncService.sync();
    }

    @GetMapping("/categories")
    public MercadonaCategoriesResponse getCategories() {
        return mercadonaClient.getCategories();
    }
}
