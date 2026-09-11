package com.micarro.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.micarro.backend.entity.Product;
import com.micarro.backend.entity.UserFavorite;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    /*
     * Consulta nativa para incluir también productos inactivos: los favoritos
     * existentes no deben "desaparecer" si un producto deja de estar activo.
     */
    @Query(
            value = """
                    select p.* from user_favorites f
                    join products p on p.id = f.product_id
                    where f.user_id = :userId
                    order by f.id
                    """,
            nativeQuery = true
    )
    List<Product> findProductsByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    long deleteByUserIdAndProductId(Long userId, Long productId);
}
