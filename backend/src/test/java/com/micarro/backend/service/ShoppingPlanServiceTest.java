package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.micarro.backend.dto.ShoppingPlanItemResponse;
import com.micarro.backend.dto.ShoppingPlanRequest;
import com.micarro.backend.dto.ShoppingPlanResponse;
import com.micarro.backend.entity.Product;
import com.micarro.backend.model.ShoppingMode;
import com.micarro.backend.strategy.BalancedShoppingStrategy;
import com.micarro.backend.strategy.CheapShoppingStrategy;
import com.micarro.backend.strategy.QualityShoppingStrategy;

@ExtendWith(MockitoExtension.class)
class ShoppingPlanServiceTest {

    @Mock
    private CandidateSelectionService candidateSelectionService;

    private ShoppingPlanService shoppingPlanService;

    @BeforeEach
    void setUp() {

        ScoringEngineService scoringEngineService = new ScoringEngineService(
                List.of(
                        new CheapShoppingStrategy(),
                        new BalancedShoppingStrategy(),
                        new QualityShoppingStrategy()
                )
        );

        shoppingPlanService = new ShoppingPlanService(
                candidateSelectionService,
                scoringEngineService,
                new ProductSelectionService(),
                new BudgetOptimizer()
        );
    }

    private Product product(
            long id,
            String name,
            String brand,
            String format,
            String price) {

        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setBrand(brand);
        product.setFormat(format);
        product.setPrice(price == null ? null : new BigDecimal(price));

        return product;
    }

    private ShoppingPlanRequest request(
            String budget,
            ShoppingMode mode,
            List<String> items) {

        ShoppingPlanRequest request = new ShoppingPlanRequest();
        request.setBudget(new BigDecimal(budget));
        request.setMode(mode);
        request.setItems(items);

        return request;
    }

    @Test
    void plan_buildsItemFromWinnerWithQuantityOneAndSubtotalEqualUnitPrice() {

        Product pollo = product(1, "Pollo entero", "Marca", "1 kg", "10.00");
        pollo.setImageUrl("https://example.com/pollo.jpg");

        ShoppingPlanRequest request =
                request("60", ShoppingMode.BALANCED, List.of("pollo"));

        when(candidateSelectionService.selectCandidates(request))
                .thenReturn(Map.of("pollo", List.of(pollo)));

        ShoppingPlanResponse response = shoppingPlanService.plan(request);

        assertThat(response.getItems()).hasSize(1);

        ShoppingPlanItemResponse item = response.getItems().get(0);

        assertThat(item.getProductId()).isEqualTo(1L);
        assertThat(item.getName()).isEqualTo("Pollo entero");
        assertThat(item.getBrand()).isEqualTo("Marca");
        assertThat(item.getFormat()).isEqualTo("1 kg");
        assertThat(item.getImageUrl()).isEqualTo("https://example.com/pollo.jpg");
        assertThat(item.getQuantity()).isEqualTo(1);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("10.00");
        assertThat(item.getSubtotal()).isEqualByComparingTo("10.00");

        assertThat(response.getEstimatedTotal()).isEqualByComparingTo("10.00");
        assertThat(response.getRemainingBudget()).isEqualByComparingTo("50.00");
    }

    @Test
    void plan_sumsSubtotalsAcrossTerms() {

        Product pollo = product(1, "Pollo", null, null, "10.00");
        Product arroz = product(2, "Arroz", null, null, "5.50");

        ShoppingPlanRequest request =
                request("60", ShoppingMode.BALANCED, List.of("pollo", "arroz"));

        Map<String, List<Product>> candidates = new LinkedHashMap<>();
        candidates.put("pollo", List.of(pollo));
        candidates.put("arroz", List.of(arroz));

        when(candidateSelectionService.selectCandidates(request))
                .thenReturn(candidates);

        ShoppingPlanResponse response = shoppingPlanService.plan(request);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getEstimatedTotal()).isEqualByComparingTo("15.50");
        assertThat(response.getRemainingBudget()).isEqualByComparingTo("44.50");
    }

    @Test
    void plan_skipsTermsWithoutWinner() {

        Product pollo = product(1, "Pollo", null, null, "10.00");

        ShoppingPlanRequest request =
                request("60", ShoppingMode.BALANCED, List.of("pollo", "setas"));

        Map<String, List<Product>> candidates = new LinkedHashMap<>();
        candidates.put("pollo", List.of(pollo));
        candidates.put("setas", List.of());

        when(candidateSelectionService.selectCandidates(request))
                .thenReturn(candidates);

        ShoppingPlanResponse response = shoppingPlanService.plan(request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getName()).isEqualTo("Pollo");
        assertThat(response.getEstimatedTotal()).isEqualByComparingTo("10.00");
    }

    @Test
    void plan_removesItemsUntilTotalFitsBudget() {

        Product pollo = product(1, "Pollo", null, null, "10.00");

        ShoppingPlanRequest request =
                request("5", ShoppingMode.BALANCED, List.of("pollo"));

        when(candidateSelectionService.selectCandidates(request))
                .thenReturn(Map.of("pollo", List.of(pollo)));

        ShoppingPlanResponse response = shoppingPlanService.plan(request);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getEstimatedTotal()).isEqualByComparingTo("0");
        assertThat(response.getRemainingBudget()).isEqualByComparingTo("5");
    }

    @Test
    void plan_appliesBudgetOptimizationKeepingCheaperAlternative() {

        Product pollo = product(1, "Pollo", null, null, "12.00");
        Product arroz = product(2, "Arroz", null, null, "8.00");

        ShoppingPlanRequest request =
                request("15", ShoppingMode.BALANCED, List.of("pollo", "arroz"));

        Map<String, List<Product>> candidates = new LinkedHashMap<>();
        candidates.put("pollo", List.of(pollo));
        candidates.put("arroz", List.of(arroz));

        when(candidateSelectionService.selectCandidates(request))
                .thenReturn(candidates);

        ShoppingPlanResponse response = shoppingPlanService.plan(request);

        // Ambos candidatos puntúan igual (un único candidato por término), así
        // que el desempate elimina primero el más caro ("pollo").
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(2L);
        assertThat(response.getEstimatedTotal()).isEqualByComparingTo("8.00");
        assertThat(response.getRemainingBudget()).isEqualByComparingTo("7.00");
    }

    @Test
    void plan_handlesProductWithoutPrice() {

        Product pollo = product(1, "Pollo", null, null, null);

        ShoppingPlanRequest request =
                request("60", ShoppingMode.BALANCED, List.of("pollo"));

        when(candidateSelectionService.selectCandidates(request))
                .thenReturn(Map.of("pollo", List.of(pollo)));

        ShoppingPlanResponse response = shoppingPlanService.plan(request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getUnitPrice()).isNull();
        assertThat(response.getItems().get(0).getSubtotal()).isNull();
        assertThat(response.getEstimatedTotal()).isEqualByComparingTo("0");
        assertThat(response.getRemainingBudget()).isEqualByComparingTo("60");
    }

    @Test
    void plan_selectsBestScoringCandidate() {

        Product exact = product(1, "Pollo", null, null, "10.00");
        Product prefix = product(2, "Pollo asado", null, null, "20.00");

        ShoppingPlanRequest request =
                request("60", ShoppingMode.BALANCED, List.of("pollo"));

        when(candidateSelectionService.selectCandidates(request))
                .thenReturn(Map.of("pollo", List.of(prefix, exact)));

        ShoppingPlanResponse response = shoppingPlanService.plan(request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(1L);
    }

    @Test
    void plan_usesModeWhenScoring() {

        // Ambos candidatos están dentro de la tolerancia de relevancia, así
        // que cada modo decide aplicando sus propios pesos.
        Product exact = product(1, "Pollo", null, null, "20.00");

        Product categoryMatch = product(2, "Muslo", null, null, "10.00");
        categoryMatch.setCategory("Pollo");

        ShoppingPlanRequest cheap =
                request("60", ShoppingMode.CHEAP, List.of("pollo"));
        when(candidateSelectionService.selectCandidates(cheap))
                .thenReturn(Map.of("pollo", List.of(exact, categoryMatch)));

        ShoppingPlanRequest quality =
                request("60", ShoppingMode.QUALITY, List.of("pollo"));
        when(candidateSelectionService.selectCandidates(quality))
                .thenReturn(Map.of("pollo", List.of(exact, categoryMatch)));

        // CHEAP favorece el más barato; QUALITY favorece la relevancia.
        assertThat(shoppingPlanService.plan(cheap).getItems().get(0).getProductId())
                .isEqualTo(2L);

        assertThat(shoppingPlanService.plan(quality).getItems().get(0).getProductId())
                .isEqualTo(1L);
    }
}
