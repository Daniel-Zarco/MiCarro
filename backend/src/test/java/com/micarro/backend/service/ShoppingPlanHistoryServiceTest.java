package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.micarro.backend.dto.ShoppingPlanHistoryItemRequest;
import com.micarro.backend.dto.ShoppingPlanHistoryRequest;
import com.micarro.backend.dto.ShoppingPlanHistoryResponse;
import com.micarro.backend.dto.ShoppingPlanHistorySummaryResponse;
import com.micarro.backend.entity.ShoppingPlanHistory;
import com.micarro.backend.entity.ShoppingPlanHistoryItem;
import com.micarro.backend.entity.User;
import com.micarro.backend.model.ShoppingMode;
import com.micarro.backend.repository.ShoppingPlanHistoryRepository;
import com.micarro.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ShoppingPlanHistoryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShoppingPlanHistoryRepository historyRepository;

    private ShoppingPlanHistoryService historyService;

    @BeforeEach
    void setUp() {
        historyService = new ShoppingPlanHistoryService(
                userRepository,
                historyRepository
        );
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setName("Dani");
        user.setEmail("dani@example.com");
        return user;
    }

    private ShoppingPlanHistory history(long id) {

        ShoppingPlanHistory history = new ShoppingPlanHistory();
        history.setId(id);
        history.setBudget(new BigDecimal("60"));
        history.setEstimatedTotal(new BigDecimal("20"));
        history.setMode(ShoppingMode.BALANCED);
        history.setCreatedAt(Instant.now());

        ShoppingPlanHistoryItem item = new ShoppingPlanHistoryItem();
        item.setProductId(10L);
        item.setProductName("Pollo");
        item.setBrand("Marca");
        item.setFormat("1 kg");
        item.setImageUrl("http://example.com/pollo.jpg");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("10"));
        item.setSubtotal(new BigDecimal("10"));

        history.addItem(item);

        return history;
    }

    private ShoppingPlanHistoryRequest request() {

        ShoppingPlanHistoryItemRequest item =
                new ShoppingPlanHistoryItemRequest();
        item.setProductId(10L);
        item.setProductName("Pollo");
        item.setBrand("Marca");
        item.setFormat("1 kg");
        item.setImageUrl("http://example.com/pollo.jpg");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("10"));
        item.setSubtotal(new BigDecimal("10"));

        ShoppingPlanHistoryRequest request = new ShoppingPlanHistoryRequest();
        request.setBudget(new BigDecimal("60"));
        request.setEstimatedTotal(new BigDecimal("20"));
        request.setMode(ShoppingMode.BALANCED);
        request.setItems(List.of(item));

        return request;
    }

    @Test
    void create_savesSnapshotOfItems() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));

        when(historyRepository.save(any(ShoppingPlanHistory.class)))
                .thenAnswer(invocation -> {
                    ShoppingPlanHistory saved = invocation.getArgument(0);
                    saved.setId(1L);
                    saved.setCreatedAt(Instant.now());
                    return saved;
                });

        ShoppingPlanHistoryResponse response =
                historyService.create("dani@example.com", request());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getBudget()).isEqualByComparingTo("60");
        assertThat(response.getMode()).isEqualTo(ShoppingMode.BALANCED);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("Pollo");
        assertThat(response.getItems().get(0).getUnitPrice())
                .isEqualByComparingTo("10");
    }

    @Test
    void getHistory_returnsSummaries() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(historyRepository.findByUserIdOrderByCreatedAtDescIdDesc(1L))
                .thenReturn(List.of(history(5)));

        List<ShoppingPlanHistorySummaryResponse> result =
                historyService.getHistory("dani@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(5L);
        assertThat(result.get(0).getEstimatedTotal())
                .isEqualByComparingTo("20");
    }

    @Test
    void getById_returnsDetailWhenOwned() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(historyRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(history(5)));

        ShoppingPlanHistoryResponse response =
                historyService.getById("dani@example.com", 5L);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void getById_throwsNotFoundWhenNotOwned() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(historyRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> historyService.getById("dani@example.com", 5L))
                .satisfies(exception -> assertThat(exception.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void delete_removesOwnedHistory() {

        ShoppingPlanHistory owned = history(5);

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(historyRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(owned));

        historyService.delete("dani@example.com", 5L);

        verify(historyRepository).delete(owned);
    }

    @Test
    void delete_throwsNotFoundWhenNotOwned() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(historyRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> historyService.delete("dani@example.com", 5L))
                .satisfies(exception -> assertThat(exception.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(historyRepository, never()).delete(any());
    }

    @Test
    void getHistory_throwsUnauthorizedForUnknownUser() {

        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> historyService.getHistory("nobody@example.com"))
                .satisfies(exception -> assertThat(exception.getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
