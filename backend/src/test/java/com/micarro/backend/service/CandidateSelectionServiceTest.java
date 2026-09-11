package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.micarro.backend.dto.ShoppingPlanRequest;
import com.micarro.backend.entity.Product;
import com.micarro.backend.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class CandidateSelectionServiceTest {

    private static final PageRequest LIMIT =
            PageRequest.of(0, CandidateSelectionService.MAX_CANDIDATES_PER_ITEM);

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CandidateSelectionService candidateSelectionService;

    private Product product(long id, String name, String brand, String category) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setBrand(brand);
        product.setCategory(category);
        return product;
    }

    private PageImpl<Product> page(Product... products) {
        return new PageImpl<Product>(List.of(products));
    }

    @Test
    void findCandidates_ordersResultsByRelevanceTiers() {

        Product exactName = product(1, "Pollo", null, null);
        Product prefixName = product(2, "Pollo asado", null, null);
        Product categoryMatch = product(3, "Muslo", null, "Pollo");
        Product containsName = product(4, "Sazonador de pollo", null, null);

        when(productRepository.findByNameIgnoreCase("pollo", LIMIT))
                .thenReturn(page(exactName));

        when(productRepository.findByNameStartingWithIgnoreCase("pollo", LIMIT))
                .thenReturn(page(exactName, prefixName));

        when(productRepository.findByCategoryContainingIgnoreCase("pollo", LIMIT))
                .thenReturn(page(categoryMatch));

        when(productRepository.findByNameContainingIgnoreCase("pollo", LIMIT))
                .thenReturn(page(exactName, prefixName, containsName));

        when(productRepository
                .findByBrandContainingIgnoreCaseOrCategoryContainingIgnoreCase("pollo", "pollo", LIMIT))
                .thenReturn(page());

        List<Product> result = candidateSelectionService.findCandidates("pollo");

        assertThat(result).containsExactly(
                exactName,
                prefixName,
                categoryMatch,
                containsName
        );
    }

    @Test
    void findCandidates_prioritizesPrefixOverContains() {

        Product prefix = product(1, "Arroz redondo", null, null);
        Product contains = product(2, "Bebida de arroz", null, null);

        when(productRepository.findByNameIgnoreCase("arroz", LIMIT))
                .thenReturn(page());

        when(productRepository.findByNameStartingWithIgnoreCase("arroz", LIMIT))
                .thenReturn(page(prefix));

        when(productRepository.findByCategoryContainingIgnoreCase("arroz", LIMIT))
                .thenReturn(page());

        when(productRepository.findByNameContainingIgnoreCase("arroz", LIMIT))
                .thenReturn(page(prefix, contains));

        when(productRepository
                .findByBrandContainingIgnoreCaseOrCategoryContainingIgnoreCase("arroz", "arroz", LIMIT))
                .thenReturn(page());

        List<Product> result = candidateSelectionService.findCandidates("arroz");

        assertThat(result).containsExactly(prefix, contains);
    }

    @Test
    void findCandidates_usesFallbackWhenNoRelevantMatches() {

        Product brandMatch = product(1, "Producto X", "Pollo", null);

        when(productRepository.findByNameIgnoreCase("pollo", LIMIT))
                .thenReturn(page());

        when(productRepository.findByNameStartingWithIgnoreCase("pollo", LIMIT))
                .thenReturn(page());

        when(productRepository.findByCategoryContainingIgnoreCase("pollo", LIMIT))
                .thenReturn(page());

        when(productRepository.findByNameContainingIgnoreCase("pollo", LIMIT))
                .thenReturn(page());

        when(productRepository
                .findByBrandContainingIgnoreCaseOrCategoryContainingIgnoreCase("pollo", "pollo", LIMIT))
                .thenReturn(page(brandMatch));

        List<Product> result = candidateSelectionService.findCandidates("pollo");

        assertThat(result).containsExactly(brandMatch);
    }

    @Test
    void findCandidates_limitsToMaxAndStopsQuerying() {

        List<Product> tenMatches = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> product(i, "Pollo " + i, null, null))
                .toList();

        when(productRepository.findByNameIgnoreCase("pollo", LIMIT))
                .thenReturn(new PageImpl<Product>(tenMatches));

        List<Product> result = candidateSelectionService.findCandidates("pollo");

        assertThat(result).hasSize(CandidateSelectionService.MAX_CANDIDATES_PER_ITEM);

        verify(productRepository, never())
                .findByNameStartingWithIgnoreCase("pollo", LIMIT);
        verify(productRepository, never())
                .findByCategoryContainingIgnoreCase("pollo", LIMIT);
        verify(productRepository, never())
                .findByNameContainingIgnoreCase("pollo", LIMIT);
        verify(productRepository, never())
                .findByBrandContainingIgnoreCaseOrCategoryContainingIgnoreCase("pollo", "pollo", LIMIT);
    }

    @Test
    void findCandidates_deduplicatesAcrossTiers() {

        Product pollo = product(1, "Pollo", "Pollo", "Pollo");

        when(productRepository.findByNameIgnoreCase("pollo", LIMIT))
                .thenReturn(page(pollo));

        when(productRepository.findByNameStartingWithIgnoreCase("pollo", LIMIT))
                .thenReturn(page(pollo));

        when(productRepository.findByCategoryContainingIgnoreCase("pollo", LIMIT))
                .thenReturn(page(pollo));

        when(productRepository.findByNameContainingIgnoreCase("pollo", LIMIT))
                .thenReturn(page(pollo));

        when(productRepository
                .findByBrandContainingIgnoreCaseOrCategoryContainingIgnoreCase("pollo", "pollo", LIMIT))
                .thenReturn(page(pollo));

        List<Product> result = candidateSelectionService.findCandidates("pollo");

        assertThat(result).containsExactly(pollo);
    }

    @Test
    void findCandidates_trimsTermBeforeSearching() {

        when(productRepository.findByNameIgnoreCase("arroz", LIMIT))
                .thenReturn(page());

        when(productRepository.findByNameStartingWithIgnoreCase("arroz", LIMIT))
                .thenReturn(page());

        when(productRepository.findByCategoryContainingIgnoreCase("arroz", LIMIT))
                .thenReturn(page());

        when(productRepository.findByNameContainingIgnoreCase("arroz", LIMIT))
                .thenReturn(page());

        when(productRepository
                .findByBrandContainingIgnoreCaseOrCategoryContainingIgnoreCase("arroz", "arroz", LIMIT))
                .thenReturn(page());

        candidateSelectionService.findCandidates("  arroz  ");

        verify(productRepository).findByNameIgnoreCase("arroz", LIMIT);
    }

    @Test
    void findCandidates_returnsEmptyForBlankTerm() {

        assertThat(candidateSelectionService.findCandidates("   ")).isEmpty();
        assertThat(candidateSelectionService.findCandidates(null)).isEmpty();

        verifyNoInteractions(productRepository);
    }

    @Test
    void selectCandidates_groupsCandidatesByTerm() {

        Product pollo = product(1, "Pollo entero", null, null);
        Product arroz = product(2, "Arroz redondo", null, null);

        stubTerm("pollo", pollo);
        stubTerm("arroz", arroz);

        ShoppingPlanRequest request = new ShoppingPlanRequest();
        request.setItems(List.of("pollo", " arroz ", ""));

        Map<String, List<Product>> result =
                candidateSelectionService.selectCandidates(request);

        assertThat(result).containsOnlyKeys("pollo", "arroz");
        assertThat(result.get("pollo")).containsExactly(pollo);
        assertThat(result.get("arroz")).containsExactly(arroz);
    }

    @Test
    void selectCandidates_returnsEmptyMapWhenNoItems() {

        ShoppingPlanRequest request = new ShoppingPlanRequest();

        assertThat(candidateSelectionService.selectCandidates(request)).isEmpty();
        assertThat(candidateSelectionService.selectCandidates(null)).isEmpty();

        verifyNoInteractions(productRepository);
    }

    @Test
    void findCandidates_findsSingularProductFromPluralTerm() {

        Product yogur = product(1, "Yogur natural", null, null);

        stubVariants(
                List.of("yogures", "yogur"),
                Map.of("yogur", List.of(yogur))
        );

        List<Product> result = candidateSelectionService.findCandidates("yogures");

        assertThat(result).containsExactly(yogur);
        verify(productRepository).findByNameContainingIgnoreCase("yogur", LIMIT);

        // Al terminar en "es" no se añade además la variante sin "s".
        verify(productRepository, never())
                .findByNameContainingIgnoreCase("yogure", LIMIT);
    }

    @Test
    void findCandidates_combinesVariantsAndDeduplicates() {

        Product first = product(1, "Producto con setas", null, null);
        Product second = product(2, "Crema de setas", null, null);

        stubVariants(
                List.of("setas", "seta"),
                Map.of(
                        "setas", List.of(first),
                        "seta", List.of(first, second)
                )
        );

        List<Product> result = candidateSelectionService.findCandidates("setas");

        assertThat(result).containsExactly(first, second);
    }

    @Test
    void findCandidates_keepsSingularTermsUnchanged() {

        Product arroz = product(1, "Arroz redondo", null, null);

        when(productRepository.findByNameIgnoreCase("arroz", LIMIT))
                .thenReturn(page(arroz));
        when(productRepository.findByNameStartingWithIgnoreCase("arroz", LIMIT))
                .thenReturn(page());
        when(productRepository.findByCategoryContainingIgnoreCase("arroz", LIMIT))
                .thenReturn(page());
        when(productRepository.findByNameContainingIgnoreCase("arroz", LIMIT))
                .thenReturn(page());
        when(productRepository
                .findByBrandContainingIgnoreCaseOrCategoryContainingIgnoreCase("arroz", "arroz", LIMIT))
                .thenReturn(page());

        List<Product> result = candidateSelectionService.findCandidates("arroz");

        assertThat(result).containsExactly(arroz);
    }

    @Test
    void findCandidates_limitsToMaxAcrossVariants() {

        List<Product> tenMatches = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> product(i, "Producto " + i, null, null))
                .toList();

        when(productRepository.findByNameIgnoreCase("setas", LIMIT)).thenReturn(page());
        when(productRepository.findByNameStartingWithIgnoreCase("setas", LIMIT)).thenReturn(page());
        when(productRepository.findByCategoryContainingIgnoreCase("setas", LIMIT)).thenReturn(page());
        when(productRepository.findByNameContainingIgnoreCase("setas", LIMIT))
                .thenReturn(new PageImpl<Product>(tenMatches));

        when(productRepository.findByNameIgnoreCase("seta", LIMIT)).thenReturn(page());
        when(productRepository.findByNameStartingWithIgnoreCase("seta", LIMIT)).thenReturn(page());
        when(productRepository.findByCategoryContainingIgnoreCase("seta", LIMIT)).thenReturn(page());

        List<Product> result = candidateSelectionService.findCandidates("setas");

        assertThat(result).hasSize(CandidateSelectionService.MAX_CANDIDATES_PER_ITEM);

        verify(productRepository, never())
                .findByNameContainingIgnoreCase("seta", LIMIT);
        verify(productRepository, never())
                .findByBrandContainingIgnoreCaseOrCategoryContainingIgnoreCase(any(), any(), any());
    }

    private void stubVariants(
            List<String> variants,
            Map<String, List<Product>> nameContainsResults) {

        for (String variant : variants) {

            List<Product> contains =
                    nameContainsResults.getOrDefault(variant, List.of());

            when(productRepository.findByNameIgnoreCase(variant, LIMIT))
                    .thenReturn(page());
            when(productRepository.findByNameStartingWithIgnoreCase(variant, LIMIT))
                    .thenReturn(page());
            when(productRepository.findByCategoryContainingIgnoreCase(variant, LIMIT))
                    .thenReturn(page());
            when(productRepository.findByNameContainingIgnoreCase(variant, LIMIT))
                    .thenReturn(new PageImpl<Product>(contains));
            when(productRepository
                    .findByBrandContainingIgnoreCaseOrCategoryContainingIgnoreCase(variant, variant, LIMIT))
                    .thenReturn(page());
        }
    }

    private void stubTerm(String term, Product exactMatch) {        when(productRepository.findByNameIgnoreCase(term, LIMIT))
                .thenReturn(page(exactMatch));
        when(productRepository.findByNameStartingWithIgnoreCase(term, LIMIT))
                .thenReturn(page());
        when(productRepository.findByCategoryContainingIgnoreCase(term, LIMIT))
                .thenReturn(page());
        when(productRepository.findByNameContainingIgnoreCase(term, LIMIT))
                .thenReturn(page());
        when(productRepository
                .findByBrandContainingIgnoreCaseOrCategoryContainingIgnoreCase(term, term, LIMIT))
                .thenReturn(page());
    }
}
