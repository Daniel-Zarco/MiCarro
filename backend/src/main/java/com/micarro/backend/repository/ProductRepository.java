package com.micarro.backend.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.micarro.backend.dto.CategoryCount;
import com.micarro.backend.dto.CategoryResponse;
import com.micarro.backend.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /*
     * Fila para bajadas/subidas: precio actual (products.price) y el precio
     * vigente hace la ventana configurada (última fila del historial con
     * recorded_at <= cutoff). Se mapea por proyección nativa.
     */
    interface ProductPriceChangeRow {
        Long getId();
        String getExternalId();
        String getName();
        String getBrand();
        String getCategory();
        String getImageUrl();
        String getFormat();
        BigDecimal getCurrentPrice();
        BigDecimal getPreviousPrice();
    }

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

    Page<Product> findByMainCategoryIgnoreCase(
            String mainCategory,
            Pageable pageable
    );

    Page<Product> findByNameContainingIgnoreCaseAndMainCategoryIgnoreCase(
            String name,
            String mainCategory,
            Pageable pageable
    );

    Page<Product> findByMainCategoryIgnoreCaseAndCategoryIn(
            String mainCategory,
            Collection<String> categories,
            Pageable pageable
    );

    Page<Product> findByNameContainingIgnoreCaseAndMainCategoryIgnoreCaseAndCategoryIn(
            String name,
            String mainCategory,
            Collection<String> categories,
            Pageable pageable
    );

    @Query("""
            select new com.micarro.backend.dto.CategoryCount(p.category, count(p))
            from Product p
            where p.mainCategory = :mainCategory
            group by p.category
            """)
    List<CategoryCount> countByCategoryInMainCategory(
            @Param("mainCategory") String mainCategory
    );

    @Query("""
            select distinct p.category
            from Product p
            where p.mainCategory = :mainCategory and p.category is not null
            """)
    List<String> findDistinctCategoryByMainCategory(
            @Param("mainCategory") String mainCategory
    );

    @Query("""
            select new com.micarro.backend.dto.CategoryResponse(p.mainCategory, count(p))
            from Product p
            where p.mainCategory is not null
            group by p.mainCategory
            order by lower(p.mainCategory)
            """)
    List<CategoryResponse> findCategorySummaries();

    Page<Product> findByActiveTrueAndFirstSeenAtGreaterThanEqual(
            Instant since,
            Pageable pageable
    );

    /*
     * Bajadas de precio: productos activos cuyo precio actual es menor que el
     * vigente hace la ventana (cutoff), ordenados por mayor % de bajada primero.
     * El filtrado y la paginación ocurren en PostgreSQL (no se filtra después).
     */
    @Query(
            value = """
                    select
                      p.id as id, p.external_id as externalId, p.name as name,
                      p.brand as brand, p.category as category, p.image_url as imageUrl,
                      p.format as format, p.price as currentPrice,
                      prev.price as previousPrice
                    from products p
                    join lateral (
                      select ph.price
                      from product_price_history ph
                      where ph.product_id = p.id and ph.recorded_at <= :cutoff
                      order by ph.recorded_at desc
                      limit 1
                    ) prev on true
                    where p.active = true
                      and p.price is not null
                      and prev.price is not null
                      and p.price <> prev.price
                      and p.price < prev.price
                    order by (p.price - prev.price) / prev.price asc
                    """,
            countQuery = """
                    select count(*)
                    from products p
                    join lateral (
                      select ph.price
                      from product_price_history ph
                      where ph.product_id = p.id and ph.recorded_at <= :cutoff
                      order by ph.recorded_at desc
                      limit 1
                    ) prev on true
                    where p.active = true
                      and p.price is not null
                      and prev.price is not null
                      and p.price <> prev.price
                      and p.price < prev.price
                    """,
            nativeQuery = true
    )
    Page<ProductPriceChangeRow> findPriceDrops(
            @Param("cutoff") Instant cutoff,
            Pageable pageable
    );

    /*
     * Subidas de precio: análogo a bajadas pero con el precio actual mayor,
     * ordenado por mayor % de subida primero.
     */
    @Query(
            value = """
                    select
                      p.id as id, p.external_id as externalId, p.name as name,
                      p.brand as brand, p.category as category, p.image_url as imageUrl,
                      p.format as format, p.price as currentPrice,
                      prev.price as previousPrice
                    from products p
                    join lateral (
                      select ph.price
                      from product_price_history ph
                      where ph.product_id = p.id and ph.recorded_at <= :cutoff
                      order by ph.recorded_at desc
                      limit 1
                    ) prev on true
                    where p.active = true
                      and p.price is not null
                      and prev.price is not null
                      and p.price <> prev.price
                      and p.price > prev.price
                    order by (p.price - prev.price) / prev.price desc
                    """,
            countQuery = """
                    select count(*)
                    from products p
                    join lateral (
                      select ph.price
                      from product_price_history ph
                      where ph.product_id = p.id and ph.recorded_at <= :cutoff
                      order by ph.recorded_at desc
                      limit 1
                    ) prev on true
                    where p.active = true
                      and p.price is not null
                      and prev.price is not null
                      and p.price <> prev.price
                      and p.price > prev.price
                    """,
            nativeQuery = true
    )
    Page<ProductPriceChangeRow> findPriceRaises(
            @Param("cutoff") Instant cutoff,
            Pageable pageable
    );
}
