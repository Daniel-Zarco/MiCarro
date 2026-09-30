package com.micarro.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.micarro.backend.entity.SavedPlan;

public interface SavedPlanRepository extends JpaRepository<SavedPlan, Long> {

    List<SavedPlan> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<SavedPlan> findByIdAndUserId(Long id, Long userId);
}