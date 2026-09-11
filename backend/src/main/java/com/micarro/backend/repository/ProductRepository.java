package com.micarro.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.micarro.backend.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByExternalId(String externalId);

    Page<Product> findByNameIgnoreCase(
            String name,
            Pageable pageable
    );

    Page<Product> findByNameStartingWithIgnoreCase(
            String name,
            Pageable pageable
    );

    Page<Product> findByCategoryContainingIgnoreCase(
            String category,
            Pageable pageable
    );

    Page<Product> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );

    Page<Product> findByBrandContainingIgnoreCaseOrCategoryContainingIgnoreCase(
            String brand,
            String category,
            Pageable pageable
    );
}
