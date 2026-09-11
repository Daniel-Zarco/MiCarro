package com.micarro.backend.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.micarro.backend.dto.ShoppingPlanRequest;
import com.micarro.backend.entity.Product;
import com.micarro.backend.repository.ProductRepository;

@Service
public class CandidateSelectionService {

    /**
     * Número máximo de candidatos que se devuelven por cada término.
     */
    static final int MAX_CANDIDATES_PER_ITEM = 10;

    private final ProductRepository productRepository;

    public CandidateSelectionService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Busca candidatos para cada producto solicitado en la petición.
     *
     * <p>Solo agrupa los productos relacionados con cada término. Todavía
     * no puntúa ni elige un ganador.</p>
     */
    public Map<String, List<Product>> selectCandidates(ShoppingPlanRequest request) {

        Map<String, List<Product>> candidatesByItem = new LinkedHashMap<>();

        if (request == null || request.getItems() == null) {
            return candidatesByItem;
        }

        for (String item : request.getItems()) {

            if (item == null || item.isBlank()) {
                continue;
            }

            String term = item.trim();

            candidatesByItem.putIfAbsent(
                    term,
                    findCandidates(term)
            );
        }

        return candidatesByItem;
    }

    /**
     * Devuelve hasta {@value #MAX_CANDIDATES_PER_ITEM} candidatos relacionados
     * con el término, ordenados por relevancia:
     *
     * <ol>
     *   <li>nombre exactamente igual al término</li>
     *   <li>nombre que empieza por el término</li>
     *   <li>categoría que contiene el término</li>
     *   <li>nombre que contiene el término</li>
     *   <li>marca/categoría como fallback</li>
     * </ol>
     *
     * <p>Para cada nivel se prueban también variantes simples de singular/plural
     * del término (por ejemplo "yogures" → "yogur"), combinando y deduplicando
     * los resultados.</p>
     */
    public List<Product> findCandidates(String term) {

        if (term == null || term.isBlank()) {
            return List.of();
        }

        String normalizedTerm = term.trim();

        List<String> variants = termVariants(normalizedTerm);

        Pageable limit = PageRequest.of(0, MAX_CANDIDATES_PER_ITEM);

        List<Product> candidates = new ArrayList<>();
        Set<Long> selectedIds = new HashSet<>();

        // 1. Nombre exactamente igual al término.
        addTier(
                candidates,
                selectedIds,
                variants,
                variant -> productRepository
                        .findByNameIgnoreCase(variant, limit)
                        .getContent()
        );

        // 2. Nombre que empieza por el término.
        if (candidates.size() < MAX_CANDIDATES_PER_ITEM) {
            addTier(
                    candidates,
                    selectedIds,
                    variants,
                    variant -> productRepository
                            .findByNameStartingWithIgnoreCase(variant, limit)
                            .getContent()
            );
        }

        // 3. Categoría que contiene el término.
        if (candidates.size() < MAX_CANDIDATES_PER_ITEM) {
            addTier(
                    candidates,
                    selectedIds,
                    variants,
                    variant -> productRepository
                            .findByCategoryContainingIgnoreCase(variant, limit)
                            .getContent()
            );
        }

        // 4. Nombre que contiene el término.
        if (candidates.size() < MAX_CANDIDATES_PER_ITEM) {
            addTier(
                    candidates,
                    selectedIds,
                    variants,
                    variant -> productRepository
                            .findByNameContainingIgnoreCase(variant, limit)
                            .getContent()
            );
        }

        // 5. Marca/categoría como fallback.
        if (candidates.size() < MAX_CANDIDATES_PER_ITEM) {
            addTier(
                    candidates,
                    selectedIds,
                    variants,
                    variant -> productRepository
                            .findByBrandContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                                    variant,
                                    variant,
                                    limit
                            )
                            .getContent()
            );
        }

        return candidates;
    }

    /*
     * Variantes simples de singular/plural. Siempre incluye el término original
     * y, si aplica, una forma sin "es" y otra sin "s".
     */
    private List<String> termVariants(String term) {

        Set<String> variants = new LinkedHashSet<>();

        variants.add(term);

        if (term.endsWith("es") && term.length() > 2) {
            variants.add(term.substring(0, term.length() - 2));
        } else if (term.endsWith("s") && term.length() > 1) {
            variants.add(term.substring(0, term.length() - 1));
        }

        return new ArrayList<>(variants);
    }

    private void addTier(
            List<Product> candidates,
            Set<Long> selectedIds,
            List<String> variants,
            Function<String, List<Product>> query) {

        for (String variant : variants) {

            if (candidates.size() >= MAX_CANDIDATES_PER_ITEM) {
                return;
            }

            addCandidates(candidates, selectedIds, query.apply(variant));
        }
    }

    private void addCandidates(
            List<Product> candidates,
            Set<Long> selectedIds,
            List<Product> found) {

        for (Product product : found) {

            if (candidates.size() >= MAX_CANDIDATES_PER_ITEM) {
                return;
            }

            if (product.getId() != null && selectedIds.add(product.getId())) {
                candidates.add(product);
            }
        }
    }
}
