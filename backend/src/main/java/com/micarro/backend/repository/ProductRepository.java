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

    /*
     * Productos realmente nuevos: activos con firstSeenAt reciente que NO
     * tengan una referencia histórica equivalente anterior (activa o inactiva,
     * cualquier source). La identidad es estricta (name + format + category +
     * brand normalizados); si la referencia anterior tiene firstSeenAt NULL se
     * usa su id inferior como evidencia histórica. Solo decide si el producto
     * aparece en "Nuevos"; no modifica ni fusiona filas.
     */
    @Query(
            value = """
                    select p.*
                    from products p
                    where p.active = true
                      and p.first_seen_at >= :since
                      and not exists (
                        select 1
                        from products prior
                        where prior.id <> p.id
                          and lower(prior.name) = lower(p.name)
                          and coalesce(lower(prior.format), '') = coalesce(lower(p.format), '')
                          and coalesce(lower(prior.category), '') = coalesce(lower(p.category), '')
                          and coalesce(lower(prior.brand), '') = coalesce(lower(p.brand), '')
                          and (
                            prior.first_seen_at < p.first_seen_at
                            or (prior.first_seen_at is null and prior.id < p.id)
                          )
                      )
                    order by p.first_seen_at desc, p.id asc
                    """,
            countQuery = """
                    select count(*)
                    from products p
                    where p.active = true
                      and p.first_seen_at >= :since
                      and not exists (
                        select 1
                        from products prior
                        where prior.id <> p.id
                          and lower(prior.name) = lower(p.name)
                          and coalesce(lower(prior.format), '') = coalesce(lower(p.format), '')
                          and coalesce(lower(prior.category), '') = coalesce(lower(p.category), '')
                          and coalesce(lower(prior.brand), '') = coalesce(lower(p.brand), '')
                          and (
                            prior.first_seen_at < p.first_seen_at
                            or (prior.first_seen_at is null and prior.id < p.id)
                          )
                      )
                    """,
            nativeQuery = true
    )
    Page<Product> findNewProducts(
            @Param("since") Instant since,
            Pageable pageable
    );

    /*
     * Búsqueda por tokens con tolerancia singular/plural. Cada token viaja como
     * una regex con límites de palabra (\m...\M) y alternativa de raíz, unidos
     * por un separador de control; en SQL se desdoblan con string_to_array +
     * unnest. Un producto coincide si TODOS los tokens están en name,
     * main_category, visual_group o category (AND). Case/accent-insensitive.
     */
    String TOKEN_SEARCH_WHERE = """
            p.active = true
            and (
              select bool_and(regexp_like(
                translate(lower(
                  coalesce(p.name, '') || ' ' ||
                  coalesce(p.main_category, '') || ' ' ||
                  coalesce(p.visual_group, '') || ' ' ||
                  coalesce(p.category, '')
                ), 'áéíóúüñ', 'aeiouun'),
                t))
              from unnest(string_to_array(:tokens, chr(1))) t
            )
            """;

    /*
     * Ranking de relevancia:
     *   0 mainCategory exacta
     *   1 visualGroup exacto
     *   2 category exacta
     *   3 TODOS los tokens en visualGroup/category
     *   4 TODOS los tokens en name
     *   5 TODOS los tokens en mainCategory (coincidencia parcial de categoría)
     *   6 resto
     * La coincidencia parcial de mainCategory NUNCA gana al nombre.
     */
    String TOKEN_RELEVANCE_ORDER = """
            order by
              case
                when translate(lower(coalesce(p.main_category, '')), 'áéíóúüñ', 'aeiouun') = :exact then 0
                when translate(lower(coalesce(p.visual_group, '')), 'áéíóúüñ', 'aeiouun') = :exact then 1
                when translate(lower(coalesce(p.category, '')), 'áéíóúüñ', 'aeiouun') = :exact then 2
                when (
                  select bool_and(regexp_like(
                    translate(lower(coalesce(p.visual_group, '') || ' ' || coalesce(p.category, '')), 'áéíóúüñ', 'aeiouun'),
                    t))
                  from unnest(string_to_array(:tokens, chr(1))) t
                ) then 3
                when (
                  select bool_and(regexp_like(
                    translate(lower(coalesce(p.name, '')), 'áéíóúüñ', 'aeiouun'),
                    t))
                  from unnest(string_to_array(:tokens, chr(1))) t
                ) then 4
                when (
                  select bool_and(regexp_like(
                    translate(lower(coalesce(p.main_category, '')), 'áéíóúüñ', 'aeiouun'),
                    t))
                  from unnest(string_to_array(:tokens, chr(1))) t
                ) then 5
                else 6
              end,
              p.catalog_order asc,
              p.id asc
            """;

    @Query(
            value = "select p.* from products p where " + TOKEN_SEARCH_WHERE + TOKEN_RELEVANCE_ORDER,
            countQuery = "select count(*) from products p where " + TOKEN_SEARCH_WHERE,
            nativeQuery = true
    )
    Page<Product> searchWithRelevance(
            @Param("tokens") String tokens,
            @Param("exact") String exact,
            Pageable pageable
    );

    @Query(
            value = "select p.* from products p where " + TOKEN_SEARCH_WHERE
                    + " order by p.price asc nulls last, p.catalog_order asc, p.id asc",
            countQuery = "select count(*) from products p where " + TOKEN_SEARCH_WHERE,
            nativeQuery = true
    )
    Page<Product> searchByPriceAsc(
            @Param("tokens") String tokens,
            Pageable pageable
    );

    @Query(
            value = "select p.* from products p where " + TOKEN_SEARCH_WHERE
                    + " order by p.price desc nulls last, p.catalog_order asc, p.id asc",
            countQuery = "select count(*) from products p where " + TOKEN_SEARCH_WHERE,
            nativeQuery = true
    )
    Page<Product> searchByPriceDesc(
            @Param("tokens") String tokens,
            Pageable pageable
    );

    @Query(
            value = "select p.* from products p where " + TOKEN_SEARCH_WHERE
                    + " order by lower(p.name) asc, p.catalog_order asc, p.id asc",
            countQuery = "select count(*) from products p where " + TOKEN_SEARCH_WHERE,
            nativeQuery = true
    )
    Page<Product> searchByNameAsc(
            @Param("tokens") String tokens,
            Pageable pageable
    );

    @Query(
            value = "select p.* from products p where " + TOKEN_SEARCH_WHERE
                    + " order by lower(p.name) desc, p.catalog_order asc, p.id asc",
            countQuery = "select count(*) from products p where " + TOKEN_SEARCH_WHERE,
            nativeQuery = true
    )
    Page<Product> searchByNameDesc(
            @Param("tokens") String tokens,
            Pageable pageable
    );
}
