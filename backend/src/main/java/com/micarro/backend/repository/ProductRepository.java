package com.micarro.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.micarro.backend.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByExternalId(String externalId);

    /*
     * Consulta nativa: incluye también productos inactivos, necesarios para la
     * sincronización (reactivar productos que reaparecen). Evita el N+1 al
     * cargar de una sola vez todos los productos de una fuente.
     */
    @Query(
            value = "select * from products where source = :source",
            nativeQuery = true
    )
    List<Product> findBySourceIncludingInactive(
            @Param("source") String source
    );

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
