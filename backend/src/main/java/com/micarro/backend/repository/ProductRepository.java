package com.micarro.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.micarro.backend.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByExternalId(String externalId);

    Page<Product> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );
}