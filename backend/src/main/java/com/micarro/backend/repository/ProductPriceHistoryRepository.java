package com.micarro.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.micarro.backend.entity.ProductPriceHistory;

public interface ProductPriceHistoryRepository
        extends JpaRepository<ProductPriceHistory, Long> {
}