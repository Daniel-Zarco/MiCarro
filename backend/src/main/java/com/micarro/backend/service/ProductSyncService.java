package com.micarro.backend.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.micarro.backend.dto.SyncResult;
import com.micarro.backend.entity.Product;
import com.micarro.backend.provider.ProductProvider;
import com.micarro.backend.repository.ProductRepository;

@Service
public class ProductSyncService {

    private final ProductProvider productProvider;
    private final ProductRepository productRepository;

    public ProductSyncService(
            ProductProvider productProvider,
            ProductRepository productRepository) {

        this.productProvider = productProvider;
        this.productRepository = productRepository;
    }

    /**
     * Sincroniza el catálogo del proveedor configurado.
     */
    public SyncResult sync() {
        return sync(productProvider.getSource());
    }

    /**
     * Sincroniza el catálogo de una fuente:
     * <ol>
     *   <li>obtiene los productos del proveedor</li>
     *   <li>carga de una sola vez los existentes de esa fuente (sin N+1)</li>
     *   <li>inserta nuevos, actualiza los que cambian y marca inactivos los
     *       desaparecidos (nunca borra físicamente)</li>
     * </ol>
     * Es idempotente: repetir sin cambios deja created/updated/deactivated a 0.
     */
    public SyncResult sync(String source) {

        List<Product> incoming = productProvider.getProducts();
        int received = incoming != null ? incoming.size() : 0;

        int errors = 0;

        // Deduplicar por externalId; un producto sin externalId es un error
        // puntual que no debe romper el resto de la sincronización.
        Map<String, Product> uniqueIncoming = new LinkedHashMap<>();

        if (incoming != null) {
            for (Product product : incoming) {

                String externalId = product.getExternalId();

                if (externalId == null || externalId.isBlank()) {
                    errors++;
                    continue;
                }

                uniqueIncoming.put(externalId, product);
            }
        }

        // Una sola consulta para todos los existentes de la fuente.
        List<Product> existing =
                productRepository.findBySourceIncludingInactive(source);

        Map<String, Product> existingByExternalId = new HashMap<>();

        for (Product product : existing) {
            if (product.getExternalId() != null) {
                existingByExternalId.put(product.getExternalId(), product);
            }
        }

        Instant now = Instant.now();

        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int deactivated = 0;

        List<Product> toSave = new ArrayList<>();
        Set<String> seenExternalIds = new HashSet<>();

        for (Map.Entry<String, Product> entry : uniqueIncoming.entrySet()) {

            String externalId = entry.getKey();
            Product incomingProduct = entry.getValue();

            seenExternalIds.add(externalId);

            try {

                Product stored = existingByExternalId.get(externalId);

                if (stored == null) {

                    toSave.add(createFrom(incomingProduct, source, now));
                    created++;

                } else {

                    boolean changed = applyChanges(stored, incomingProduct);

                    stored.setSource(source);
                    stored.setActive(true);
                    stored.setLastSyncedAt(now);

                    toSave.add(stored);

                    if (changed) {
                        updated++;
                    } else {
                        unchanged++;
                    }
                }

            } catch (RuntimeException exception) {
                // Un producto problemático no debe destruir la sincronización.
                errors++;
            }
        }

        // Los productos de la fuente que ya no aparecen se desactivan.
        for (Product stored : existing) {

            String externalId = stored.getExternalId();

            if (externalId == null || seenExternalIds.contains(externalId)) {
                continue;
            }

            if (stored.isActive()) {
                stored.setActive(false);
                toSave.add(stored);
                deactivated++;
            }
        }

        persist(toSave);

        return new SyncResult(
                received,
                created,
                updated,
                unchanged,
                deactivated,
                errors
        );
    }

    private Product createFrom(Product incoming, String source, Instant now) {

        Product product = new Product();

        product.setExternalId(incoming.getExternalId());
        product.setName(incoming.getName());
        product.setBrand(incoming.getBrand());
        product.setCategory(incoming.getCategory());
        product.setImageUrl(incoming.getImageUrl());
        product.setFormat(incoming.getFormat());
        product.setPrice(incoming.getPrice());
        product.setSource(source);
        product.setActive(true);
        product.setLastSyncedAt(now);

        return product;
    }

    /*
     * Actualiza los campos sincronizables. Nunca cambia el externalId.
     */
    private boolean applyChanges(Product stored, Product incoming) {

        boolean changed = false;

        if (!Objects.equals(stored.getName(), incoming.getName())) {
            stored.setName(incoming.getName());
            changed = true;
        }

        if (!Objects.equals(stored.getCategory(), incoming.getCategory())) {
            stored.setCategory(incoming.getCategory());
            changed = true;
        }

        if (!Objects.equals(stored.getImageUrl(), incoming.getImageUrl())) {
            stored.setImageUrl(incoming.getImageUrl());
            changed = true;
        }

        if (!Objects.equals(stored.getFormat(), incoming.getFormat())) {
            stored.setFormat(incoming.getFormat());
            changed = true;
        }

        if (!samePrice(stored.getPrice(), incoming.getPrice())) {
            stored.setPrice(incoming.getPrice());
            changed = true;
        }

        return changed;
    }

    private boolean samePrice(BigDecimal first, BigDecimal second) {

        if (first == null || second == null) {
            return first == second;
        }

        return first.compareTo(second) == 0;
    }

    private void persist(List<Product> products) {

        if (products.isEmpty()) {
            return;
        }

        try {
            productRepository.saveAll(products);

        } catch (RuntimeException exception) {

            // Fallback: si un producto rompe el batch, guardar el resto uno a uno.
            for (Product product : products) {
                try {
                    productRepository.save(product);
                } catch (RuntimeException ignored) {
                    // Se ignora el producto problemático.
                }
            }
        }
    }
}
