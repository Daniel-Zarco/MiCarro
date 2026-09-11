package com.micarro.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.micarro.backend.entity.ShoppingPlanHistory;

public interface ShoppingPlanHistoryRepository
        extends JpaRepository<ShoppingPlanHistory, Long> {

    List<ShoppingPlanHistory> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<ShoppingPlanHistory> findByIdAndUserId(Long id, Long userId);
}
