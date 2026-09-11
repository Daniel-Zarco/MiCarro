package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.micarro.backend.entity.Product;
import com.micarro.backend.model.ProductScore;

class ProductSelectionServiceTest {

    private final ProductSelectionService productSelectionService =
            new ProductSelectionService();

    private Product product(long id) {
        Product product = new Product();
        product.setId(id);
        return product;
    }

    private ProductScore score(
            long productId,
            double finalScore,
            double relevanceScore,
            double priceScore) {

        return new ProductScore(
                product(productId),
                relevanceScore,
                priceScore,
                0.0,
                finalScore
        );
    }

    @Test
    void selectBest_returnsEmptyWhenNoScores() {

        assertThat(productSelectionService.selectBest("pollo", List.of()))
                .isEmpty();

        assertThat(productSelectionService.selectBest("pollo", null))
                .isEmpty();
    }

    @Test
    void selectBest_picksHighestFinalScoreWithinRelevanceTolerance() {

        ProductScore low = score(1, 0.4, 0.9, 0.0);
        ProductScore high = score(2, 0.9, 0.8, 1.0);

        assertThat(productSelectionService.selectBest("pollo", List.of(low, high)))
                .contains(high);
    }

    @Test
    void selectBest_breaksTieByRelevance() {

        ProductScore lowRelevance = score(1, 0.8, 0.85, 1.0);
        ProductScore highRelevance = score(2, 0.8, 0.90, 0.0);

        assertThat(productSelectionService.selectBest(
                "pollo", List.of(lowRelevance, highRelevance)))
                .contains(highRelevance);
    }

    @Test
    void selectBest_ignoresMuchCheaperButClearlyLessRelevantCandidate() {

        ProductScore relevantExpensive = score(1, 0.40, 1.00, 0.0);
        ProductScore cheapIrrelevant = score(2, 0.90, 0.50, 1.0);

        assertThat(productSelectionService.selectBest(
                "pollo", List.of(cheapIrrelevant, relevantExpensive)))
                .contains(relevantExpensive);
    }

    @Test
    void selectBest_considersCandidateAtRelevanceToleranceBoundary() {

        ProductScore mostRelevant = score(1, 0.5, 1.00, 0.0);
        ProductScore withinTolerance = score(2, 0.9, 0.85, 1.0);

        assertThat(productSelectionService.selectBest(
                "pollo", List.of(mostRelevant, withinTolerance)))
                .contains(withinTolerance);
    }

    @Test
    void selectBest_excludesCandidateBeyondRelevanceTolerance() {

        ProductScore mostRelevant = score(1, 0.2, 1.00, 0.0);
        ProductScore beyondTolerance = score(2, 0.9, 0.84, 1.0);

        assertThat(productSelectionService.selectBest(
                "pollo", List.of(mostRelevant, beyondTolerance)))
                .contains(mostRelevant);
    }

    @Test
    void selectBest_breaksTieByPrice() {

        ProductScore lowPrice = score(1, 0.8, 1.0, 0.2);
        ProductScore highPrice = score(2, 0.8, 1.0, 0.9);

        assertThat(productSelectionService.selectBest(
                "pollo", List.of(lowPrice, highPrice)))
                .contains(highPrice);
    }

    @Test
    void selectBest_breaksFullTieByLowestProductId() {

        ProductScore third = score(3, 0.8, 1.0, 1.0);
        ProductScore first = score(1, 0.8, 1.0, 1.0);
        ProductScore second = score(2, 0.8, 1.0, 1.0);

        assertThat(productSelectionService.selectBest(
                "pollo", List.of(third, first, second)))
                .contains(first);
    }

    @Test
    void selectBest_favoriteWithLowRelevanceDoesNotBeatSemanticFilter() {

        // El favorito tiene mayor finalScore (por favoriteScore) pero es mucho
        // menos relevante, así que el filtro semántico lo descarta.
        ProductScore relevant = score(1, 0.55, 1.00, 0.0);
        ProductScore favoriteButIrrelevant = score(2, 0.80, 0.50, 1.0);

        assertThat(productSelectionService.selectBest(
                "pollo", List.of(relevant, favoriteButIrrelevant)))
                .contains(relevant);
    }

    @Test
    void selectBestPerTerm_returnsWinnerForEachTerm() {

        ProductScore polloWinner = score(1, 0.9, 1.0, 0.5);
        ProductScore polloOther = score(2, 0.5, 0.5, 1.0);
        ProductScore arrozWinner = score(3, 0.7, 1.0, 0.0);

        Map<String, List<ProductScore>> scoresByTerm = Map.of(
                "pollo", List.of(polloOther, polloWinner),
                "arroz", List.of(arrozWinner)
        );

        Map<String, ProductScore> winners =
                productSelectionService.selectBestPerTerm(scoresByTerm);

        assertThat(winners).containsOnlyKeys("pollo", "arroz");
        assertThat(winners.get("pollo")).isEqualTo(polloWinner);
        assertThat(winners.get("arroz")).isEqualTo(arrozWinner);
    }

    @Test
    void selectBestPerTerm_skipsTermsWithoutCandidates() {

        Map<String, List<ProductScore>> scoresByTerm = Map.of(
                "pollo", List.of(score(1, 0.9, 1.0, 0.5)),
                "setas", List.of()
        );

        Map<String, ProductScore> winners =
                productSelectionService.selectBestPerTerm(scoresByTerm);

        assertThat(winners).containsOnlyKeys("pollo");
    }

    @Test
    void selectBestPerTerm_returnsEmptyMapForEmptyInput() {

        assertThat(productSelectionService.selectBestPerTerm(Map.of())).isEmpty();
        assertThat(productSelectionService.selectBestPerTerm(null)).isEmpty();
    }
}
