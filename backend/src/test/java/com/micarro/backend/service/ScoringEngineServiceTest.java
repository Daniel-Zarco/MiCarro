package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.micarro.backend.dto.ShoppingPlanRequest;
import com.micarro.backend.entity.Product;
import com.micarro.backend.model.ProductScore;
import com.micarro.backend.model.ShoppingMode;
import com.micarro.backend.strategy.BalancedShoppingStrategy;
import com.micarro.backend.strategy.CheapShoppingStrategy;
import com.micarro.backend.strategy.QualityShoppingStrategy;

class ScoringEngineServiceTest {

    private static final CheapShoppingStrategy CHEAP = new CheapShoppingStrategy();
    private static final BalancedShoppingStrategy BALANCED = new BalancedShoppingStrategy();
    private static final QualityShoppingStrategy QUALITY = new QualityShoppingStrategy();

    private final ScoringEngineService scoringEngineService =
            new ScoringEngineService(List.of(CHEAP, BALANCED, QUALITY));

    private Product product(
            long id,
            String name,
            String brand,
            String category,
            String price) {

        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setBrand(brand);
        product.setCategory(category);
        product.setPrice(price == null ? null : new BigDecimal(price));

        return product;
    }

    private ShoppingPlanRequest request(ShoppingMode mode) {
        ShoppingPlanRequest request = new ShoppingPlanRequest();
        request.setMode(mode);
        return request;
    }

    private ShoppingPlanRequest balancedRequest() {
        return request(ShoppingMode.BALANCED);
    }

    /*
     * Escenario común para comparar modos:
     * - "Pollo" (relevancia exacta, más caro)
     * - "Pollo asado" (prefijo, más barato)
     */
    private List<Product> modeScenario() {
        Product relevantExpensive = product(1, "Pollo", null, null, "20");
        Product cheapLessRelevant = product(2, "Pollo asado", null, "Carnes", "10");
        return List.of(relevantExpensive, cheapLessRelevant);
    }

    @Test
    void scoreCandidates_assignsRelevanceByMatchType() {

        Product exact = product(1, "Pollo", null, null, "10");
        Product prefix = product(2, "Pollo asado", null, null, "10");
        Product category = product(3, "Muslo", null, "Pollo", "10");
        Product contains = product(4, "Sazonador de pollo", null, null, "10");
        Product fallback = product(5, "Producto X", "Pollo", null, "10");

        List<ProductScore> scores = scoringEngineService.scoreCandidates(
                "pollo",
                List.of(exact, prefix, category, contains, fallback),
                balancedRequest()
        );

        assertThat(scores)
                .extracting(ProductScore::getRelevanceScore)
                .containsExactly(
                        ScoringEngineService.RELEVANCE_EXACT,
                        ScoringEngineService.RELEVANCE_PREFIX,
                        ScoringEngineService.RELEVANCE_CATEGORY,
                        ScoringEngineService.RELEVANCE_CONTAINS,
                        ScoringEngineService.RELEVANCE_FALLBACK
                );
    }

    @Test
    void scoreCandidates_prefixWithMatchingCategoryRanksAboveCategoryAndPrefixAlone() {

        Product exact = product(1, "Pollo", null, null, "10");
        Product prefixAndCategory = product(2, "Pollo entero", null, "Pollo", "10");
        Product categoryOnly = product(3, "Muslo", null, "Pollo", "10");
        Product prefixOnly = product(4, "Pollo asado", null, "Carnes", "10");

        List<ProductScore> scores = scoringEngineService.scoreCandidates(
                "pollo",
                List.of(exact, prefixAndCategory, categoryOnly, prefixOnly),
                balancedRequest()
        );

        assertThat(scores)
                .extracting(ProductScore::getRelevanceScore)
                .containsExactly(
                        ScoringEngineService.RELEVANCE_EXACT,
                        ScoringEngineService.RELEVANCE_PREFIX_AND_CATEGORY,
                        ScoringEngineService.RELEVANCE_CATEGORY,
                        ScoringEngineService.RELEVANCE_PREFIX
                );
    }

    @Test
    void scoreCandidates_relevanceIsCaseInsensitive() {

        Product pollo = product(1, "POLLO ENTERO", null, null, "10");

        List<ProductScore> scores = scoringEngineService.scoreCandidates(
                "PoLlO",
                List.of(pollo),
                balancedRequest()
        );

        assertThat(scores.get(0).getRelevanceScore())
                .isEqualTo(ScoringEngineService.RELEVANCE_PREFIX);
    }

    @Test
    void scoreCandidates_lowerPriceGetsHigherPriceScore() {

        Product cheap = product(1, "Pollo barato", null, null, "10");
        Product expensive = product(2, "Pollo caro", null, null, "20");

        List<ProductScore> scores = scoringEngineService.scoreCandidates(
                "pollo",
                List.of(cheap, expensive),
                balancedRequest()
        );

        assertThat(scores.get(0).getPriceScore()).isCloseTo(1.0, within(1e-9));
        assertThat(scores.get(1).getPriceScore()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void scoreCandidates_middlePriceGetsIntermediatePriceScore() {

        Product cheap = product(1, "Pollo 1", null, null, "10");
        Product middle = product(2, "Pollo 2", null, null, "15");
        Product expensive = product(3, "Pollo 3", null, null, "20");

        List<ProductScore> scores = scoringEngineService.scoreCandidates(
                "pollo",
                List.of(cheap, middle, expensive),
                balancedRequest()
        );

        assertThat(scores.get(1).getPriceScore()).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void scoreCandidates_allEqualPricesGetBestPriceScore() {

        Product first = product(1, "Pollo 1", null, null, "10");
        Product second = product(2, "Pollo 2", null, null, "10");

        List<ProductScore> scores = scoringEngineService.scoreCandidates(
                "pollo",
                List.of(first, second),
                balancedRequest()
        );

        assertThat(scores)
                .extracting(ProductScore::getPriceScore)
                .containsOnly(1.0);
    }

    @Test
    void scoreCandidates_nullPriceGetsZeroPriceScore() {

        Product withoutPrice = product(1, "Pollo", null, null, null);
        Product withPrice = product(2, "Pollo asado", null, null, "10");

        List<ProductScore> scores = scoringEngineService.scoreCandidates(
                "pollo",
                List.of(withoutPrice, withPrice),
                balancedRequest()
        );

        assertThat(scores.get(0).getPriceScore()).isZero();
    }

    @Test
    void scoreCandidates_combinesRelevanceAndPriceInFinalScore() {

        Product exact = product(1, "Pollo", null, null, "10");
        Product prefix = product(2, "Pollo asado", null, null, "20");

        List<ProductScore> scores = scoringEngineService.scoreCandidates(
                "pollo",
                List.of(exact, prefix),
                balancedRequest()
        );

        double expectedExact =
                ScoringEngineService.RELEVANCE_EXACT * BALANCED.relevanceWeight()
                        + 1.0 * BALANCED.priceWeight();

        double expectedPrefix =
                ScoringEngineService.RELEVANCE_PREFIX * BALANCED.relevanceWeight()
                        + 0.0 * BALANCED.priceWeight();

        assertThat(scores.get(0).getFinalScore()).isCloseTo(expectedExact, within(1e-9));
        assertThat(scores.get(1).getFinalScore()).isCloseTo(expectedPrefix, within(1e-9));
    }

    @Test
    void scoreCandidates_appliesCheapWeights() {

        List<ProductScore> scores = scoringEngineService.scoreCandidates(
                "pollo",
                modeScenario(),
                request(ShoppingMode.CHEAP)
        );

        assertThat(scores.get(0).getFinalScore())
                .isCloseTo(
                        ScoringEngineService.RELEVANCE_EXACT * CHEAP.relevanceWeight()
                                + 0.0 * CHEAP.priceWeight(),
                        within(1e-9));

        assertThat(scores.get(1).getFinalScore())
                .isCloseTo(
                        ScoringEngineService.RELEVANCE_PREFIX * CHEAP.relevanceWeight()
                                + 1.0 * CHEAP.priceWeight(),
                        within(1e-9));
    }

    @Test
    void scoreCandidates_appliesBalancedWeights() {

        List<ProductScore> scores = scoringEngineService.scoreCandidates(
                "pollo",
                modeScenario(),
                request(ShoppingMode.BALANCED)
        );

        assertThat(scores.get(0).getFinalScore())
                .isCloseTo(
                        ScoringEngineService.RELEVANCE_EXACT * BALANCED.relevanceWeight()
                                + 0.0 * BALANCED.priceWeight(),
                        within(1e-9));

        assertThat(scores.get(1).getFinalScore())
                .isCloseTo(
                        ScoringEngineService.RELEVANCE_PREFIX * BALANCED.relevanceWeight()
                                + 1.0 * BALANCED.priceWeight(),
                        within(1e-9));
    }

    @Test
    void scoreCandidates_appliesQualityWeights() {

        List<ProductScore> scores = scoringEngineService.scoreCandidates(
                "pollo",
                modeScenario(),
                request(ShoppingMode.QUALITY)
        );

        assertThat(scores.get(0).getFinalScore())
                .isCloseTo(
                        ScoringEngineService.RELEVANCE_EXACT * QUALITY.relevanceWeight()
                                + 0.0 * QUALITY.priceWeight(),
                        within(1e-9));

        assertThat(scores.get(1).getFinalScore())
                .isCloseTo(
                        ScoringEngineService.RELEVANCE_PREFIX * QUALITY.relevanceWeight()
                                + 1.0 * QUALITY.priceWeight(),
                        within(1e-9));
    }

    @Test
    void scoreCandidates_modesProduceDifferentResults() {

        List<ProductScore> cheap = scoringEngineService.scoreCandidates(
                "pollo", modeScenario(), request(ShoppingMode.CHEAP));

        List<ProductScore> balanced = scoringEngineService.scoreCandidates(
                "pollo", modeScenario(), request(ShoppingMode.BALANCED));

        List<ProductScore> quality = scoringEngineService.scoreCandidates(
                "pollo", modeScenario(), request(ShoppingMode.QUALITY));

        double cheapExact = cheap.get(0).getFinalScore();
        double balancedExact = balanced.get(0).getFinalScore();
        double qualityExact = quality.get(0).getFinalScore();

        assertThat(cheapExact).isNotEqualTo(balancedExact);
        assertThat(balancedExact).isNotEqualTo(qualityExact);

        // CHEAP y BALANCED favorecen el producto más barato...
        assertThat(cheap.get(1).getFinalScore()).isGreaterThan(cheapExact);
        assertThat(balanced.get(1).getFinalScore()).isGreaterThan(balancedExact);

        // ...mientras que QUALITY prioriza la relevancia.
        assertThat(qualityExact).isGreaterThan(quality.get(1).getFinalScore());
    }

    @Test
    void scoreCandidates_defaultsToBalancedWhenModeIsMissing() {

        List<ProductScore> scores = scoringEngineService.scoreCandidates(
                "pollo",
                modeScenario(),
                new ShoppingPlanRequest()
        );

        assertThat(scores.get(0).getFinalScore())
                .isCloseTo(
                        ScoringEngineService.RELEVANCE_EXACT * BALANCED.relevanceWeight()
                                + 0.0 * BALANCED.priceWeight(),
                        within(1e-9));
    }

    @Test
    void scoreCandidates_relevanceWeighsMoreThanPriceInBalanced() {

        Product relevant = product(1, "Pollo", null, null, "20");
        Product irrelevant = product(2, "Sazonador de pollo", null, null, "10");

        List<ProductScore> scores = scoringEngineService.scoreCandidates(
                "pollo",
                List.of(relevant, irrelevant),
                balancedRequest()
        );

        // Aunque sea más caro, la coincidencia exacta de nombre pesa más.
        assertThat(scores.get(0).getFinalScore())
                .isGreaterThan(scores.get(1).getFinalScore());
    }

    @Test
    void scoreCandidates_favoriteScoreIsZeroForNow() {
        Product pollo = product(1, "Pollo", null, null, "10");

        List<ProductScore> scores = scoringEngineService.scoreCandidates(
                "pollo",
                List.of(pollo),
                balancedRequest()
        );

        assertThat(scores.get(0).getFavoriteScore()).isZero();
    }

    @Test
    void scoreCandidates_preservesCandidateOrder() {

        Product first = product(1, "Pollo asado", null, null, "10");
        Product second = product(2, "Pollo", null, null, "20");

        List<ProductScore> scores = scoringEngineService.scoreCandidates(
                "pollo",
                List.of(first, second),
                balancedRequest()
        );

        assertThat(scores)
                .extracting(score -> score.getProduct().getId())
                .containsExactly(1L, 2L);
    }

    @Test
    void scoreCandidates_returnsEmptyWhenNoCandidates() {

        assertThat(scoringEngineService.scoreCandidates(
                "pollo", List.of(), balancedRequest()))
                .isEmpty();

        assertThat(scoringEngineService.scoreCandidates(
                "pollo", null, balancedRequest()))
                .isEmpty();
    }
}
