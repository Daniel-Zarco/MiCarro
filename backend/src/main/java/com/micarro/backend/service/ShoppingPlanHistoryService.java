package com.micarro.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.micarro.backend.dto.ShoppingPlanHistoryItemRequest;
import com.micarro.backend.dto.ShoppingPlanHistoryItemResponse;
import com.micarro.backend.dto.ShoppingPlanHistoryRequest;
import com.micarro.backend.dto.ShoppingPlanHistoryResponse;
import com.micarro.backend.dto.ShoppingPlanHistorySummaryResponse;
import com.micarro.backend.entity.ShoppingPlanHistory;
import com.micarro.backend.entity.ShoppingPlanHistoryItem;
import com.micarro.backend.entity.User;
import com.micarro.backend.repository.ShoppingPlanHistoryRepository;
import com.micarro.backend.repository.UserRepository;

@Service
public class ShoppingPlanHistoryService {

    private final UserRepository userRepository;
    private final ShoppingPlanHistoryRepository historyRepository;

    public ShoppingPlanHistoryService(
            UserRepository userRepository,
            ShoppingPlanHistoryRepository historyRepository) {

        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public ShoppingPlanHistoryResponse create(
            String email,
            ShoppingPlanHistoryRequest request) {

        User user = requireUser(email);

        ShoppingPlanHistory history = new ShoppingPlanHistory();
        history.setUser(user);
        history.setBudget(request.getBudget());
        history.setEstimatedTotal(request.getEstimatedTotal());
        history.setMode(request.getMode());

        if (request.getItems() != null) {

            for (ShoppingPlanHistoryItemRequest itemRequest : request.getItems()) {
                history.addItem(toItem(itemRequest));
            }
        }

        ShoppingPlanHistory saved = historyRepository.save(history);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ShoppingPlanHistorySummaryResponse> getHistory(String email) {

        User user = requireUser(email);

        return historyRepository
                .findByUserIdOrderByCreatedAtDescIdDesc(user.getId())
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShoppingPlanHistoryResponse getById(String email, Long id) {

        User user = requireUser(email);

        ShoppingPlanHistory history = findOwnedHistory(user.getId(), id);

        return toResponse(history);
    }

    @Transactional
    public void delete(String email, Long id) {

        User user = requireUser(email);

        ShoppingPlanHistory history = findOwnedHistory(user.getId(), id);

        historyRepository.delete(history);
    }

    private ShoppingPlanHistory findOwnedHistory(Long userId, Long id) {

        return historyRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Historial no encontrado"
                ));
    }

    private User requireUser(String email) {

        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario no autenticado"
                ));
    }

    private ShoppingPlanHistoryItem toItem(
            ShoppingPlanHistoryItemRequest request) {

        ShoppingPlanHistoryItem item = new ShoppingPlanHistoryItem();

        item.setProductId(request.getProductId());
        item.setProductName(request.getProductName());
        item.setBrand(request.getBrand());
        item.setFormat(request.getFormat());
        item.setImageUrl(request.getImageUrl());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(request.getUnitPrice());
        item.setSubtotal(request.getSubtotal());

        return item;
    }

    private ShoppingPlanHistoryResponse toResponse(
            ShoppingPlanHistory history) {

        List<ShoppingPlanHistoryItemResponse> items = history.getItems()
                .stream()
                .map(item -> new ShoppingPlanHistoryItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getBrand(),
                        item.getFormat(),
                        item.getImageUrl(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return new ShoppingPlanHistoryResponse(
                history.getId(),
                history.getBudget(),
                history.getEstimatedTotal(),
                history.getMode(),
                history.getCreatedAt(),
                items
        );
    }

    private ShoppingPlanHistorySummaryResponse toSummary(
            ShoppingPlanHistory history) {

        return new ShoppingPlanHistorySummaryResponse(
                history.getId(),
                history.getBudget(),
                history.getEstimatedTotal(),
                history.getMode(),
                history.getCreatedAt()
        );
    }
}
