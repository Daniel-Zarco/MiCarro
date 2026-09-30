package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.micarro.backend.dto.SavedPlanItemRequest;
import com.micarro.backend.dto.SavedPlanItemResponse;
import com.micarro.backend.dto.SavedPlanNameRequest;
import com.micarro.backend.dto.SavedPlanRequest;
import com.micarro.backend.dto.SavedPlanResponse;
import com.micarro.backend.entity.Product;
import com.micarro.backend.entity.SavedPlan;
import com.micarro.backend.entity.SavedPlanItem;
import com.micarro.backend.entity.User;
import com.micarro.backend.repository.ProductRepository;
import com.micarro.backend.repository.SavedPlanRepository;
import com.micarro.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SavedPlanServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SavedPlanRepository savedPlanRepository;

    private SavedPlanService savedPlanService;

    @BeforeEach
    void setUp() {
        savedPlanService = new SavedPlanService(
                userRepository,
                productRepository,
                savedPlanRepository
        );
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setName("Dani");
        user.setEmail("dani@example.com");
        return user;
    }

    private Product product(long id, String name, String price) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setBrand("Marca " + id);
        product.setFormat("1 ud.");
        product.setImageUrl("http://example.com/" + id + ".jpg");
        product.setPrice(new BigDecimal(price));
        return product;
    }

    private SavedPlanItemRequest itemRequest(long productId, int quantity) {
        SavedPlanItemRequest item = new SavedPlanItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private SavedPlanRequest request(String name, SavedPlanItemRequest... items) {
        SavedPlanRequest request = new SavedPlanRequest();
        request.setName(name);
        request.setItems(List.of(items));
        return request;
    }

    private SavedPlan plan(long id) {
        SavedPlan plan = new SavedPlan();
        plan.setId(id);
        plan.setName("Mi plan");
        plan.setUser(user());

        SavedPlanItem item = new SavedPlanItem();
        item.setId(10L);
        item.setPlan(plan);
        item.setProductId(99L);
        item.setProductName("Legumbres");
        item.setBrand("Marca 99");
        item.setFormat("750 g");
        item.setImageUrl("http://example.com/99.jpg");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("1.50"));
        item.setSubtotal(new BigDecimal("3.00"));
        plan.addItem(item);

        return plan;
    }

    @Test
    void create_buildsSnapshotFromCatalog() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(productRepository.findById(99L))
                .thenReturn(Optional.of(product(99L, "Legumbres", "1.50")));
        when(savedPlanRepository.save(any(SavedPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SavedPlanResponse response = savedPlanService.create(
                "dani@example.com",
                request("Mi plan", itemRequest(99L, 2))
        );

        assertThat(response.getName()).isEqualTo("Mi plan");

        SavedPlanItemResponse item = response.getItems().get(0);
        assertThat(item.getProductId()).isEqualTo(99L);
        assertThat(item.getProductName()).isEqualTo("Legumbres");
        assertThat(item.getBrand()).isEqualTo("Marca 99");
        assertThat(item.getFormat()).isEqualTo("1 ud.");
        assertThat(item.getImageUrl()).isEqualTo("http://example.com/99.jpg");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("1.50");
        assertThat(item.getSubtotal()).isEqualByComparingTo("3.00");
    }

    @Test
    void create_rejectsUnknownProduct() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> savedPlanService.create(
                        "dani@example.com",
                        request("Mi plan", itemRequest(999L, 1))
                ))
                .satisfies(exception -> assertThat(exception.getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(savedPlanRepository, never()).save(any());
    }

    @Test
    void create_throwsUnauthorizedForUnknownUser() {

        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> savedPlanService.create(
                        "nobody@example.com",
                        request("Mi plan", itemRequest(99L, 1))
                ))
                .satisfies(exception -> assertThat(exception.getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void getPlanById_returnsHistoricalAndCurrentPrice() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(savedPlanRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(plan(5L)));
        // El producto sigue activo pero con un precio actual distinto al guardado.
        when(productRepository.findById(99L))
                .thenReturn(Optional.of(product(99L, "Legumbres", "2.40")));

        SavedPlanResponse response = savedPlanService.getPlanById(
                "dani@example.com",
                5L
        );

        SavedPlanItemResponse item = response.getItems().get(0);
        // Precio histórico del snapshot.
        assertThat(item.getUnitPrice()).isEqualByComparingTo("1.50");
        assertThat(item.getSubtotal()).isEqualByComparingTo("3.00");
        // Precio actual del catálogo para añadir al carrito.
        assertThat(item.getCurrentProduct()).isNotNull();
        assertThat(item.getCurrentProduct().getPrice())
                .isEqualByComparingTo("2.40");
    }

    @Test
    void getPlanById_returnsNullCurrentProductWhenInactiveOrMissing() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(savedPlanRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(plan(5L)));
        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        SavedPlanResponse response = savedPlanService.getPlanById(
                "dani@example.com",
                5L
        );

        assertThat(response.getItems().get(0).getCurrentProduct()).isNull();
    }

    @Test
    void getPlanById_throwsNotFoundForOtherUser() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(savedPlanRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> savedPlanService.getPlanById(
                        "dani@example.com",
                        5L
                ))
                .satisfies(exception -> assertThat(exception.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void delete_throwsNotFoundForOtherUser() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(savedPlanRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> savedPlanService.delete(
                        "dani@example.com",
                        5L
                ))
                .satisfies(exception -> assertThat(exception.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(savedPlanRepository, never()).delete(any());
    }

    @Test
    void getPlans_returnsOnlyOwnPlans() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(savedPlanRepository.findByUserIdOrderByCreatedAtDescIdDesc(1L))
                .thenReturn(List.of(plan(5L)));

        var summaries = savedPlanService.getPlans("dani@example.com");

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getName()).isEqualTo("Mi plan");
        assertThat(summaries.get(0).getItemCount()).isEqualTo(1);
        assertThat(summaries.get(0).getTotal()).isEqualByComparingTo("3.00");
    }

    @Test
    void rename_updatesOwnPlanName() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(savedPlanRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(plan(5L)));
        when(savedPlanRepository.save(any(SavedPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SavedPlanNameRequest request = new SavedPlanNameRequest();
        request.setName("  Compra navideña  ");

        SavedPlanResponse response = savedPlanService.rename(
                "dani@example.com",
                5L,
                request
        );

        assertThat(response.getName()).isEqualTo("Compra navideña");
        assertThat(response.getId()).isEqualTo(5L);
    }

    @Test
    void rename_throwsNotFoundForOtherUser() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(savedPlanRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.empty());

        SavedPlanNameRequest request = new SavedPlanNameRequest();
        request.setName("Otro");

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> savedPlanService.rename(
                        "dani@example.com",
                        5L,
                        request
                ))
                .satisfies(exception -> assertThat(exception.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(savedPlanRepository, never()).save(any());
    }
}