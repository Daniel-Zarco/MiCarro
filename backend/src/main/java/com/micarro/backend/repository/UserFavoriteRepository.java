package com.micarro.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.micarro.backend.entity.Product;
import com.micarro.backend.entity.UserFavorite;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    @Query("""
            select favorite.product
            from UserFavorite favorite
            where favorite.user.id = :userId
            order by favorite.id
            """)
    List<Product> findProductsByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    long deleteByUserIdAndProductId(Long userId, Long productId);
}
