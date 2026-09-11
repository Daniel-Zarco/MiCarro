package com.micarro.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.micarro.backend.entity.Product;
import com.micarro.backend.entity.User;
import com.micarro.backend.entity.UserFavorite;
import com.micarro.backend.repository.ProductRepository;
import com.micarro.backend.repository.UserFavoriteRepository;
import com.micarro.backend.repository.UserRepository;

@Service
public class FavoriteService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final UserFavoriteRepository userFavoriteRepository;

    public FavoriteService(
            UserRepository userRepository,
            ProductRepository productRepository,
            UserFavoriteRepository userFavoriteRepository) {

        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.userFavoriteRepository = userFavoriteRepository;
    }

    public List<Product> getFavorites(String email) {

        User user = requireUser(email);

        return userFavoriteRepository.findProductsByUserId(user.getId());
    }

    @Transactional
    public Product addFavorite(String email, Long productId) {

        User user = requireUser(email);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Producto no encontrado"
                ));

        boolean alreadyFavorite =
                userFavoriteRepository.existsByUserIdAndProductId(
                        user.getId(),
                        productId
                );

        if (!alreadyFavorite) {

            UserFavorite favorite = new UserFavorite();
            favorite.setUser(user);
            favorite.setProduct(product);

            userFavoriteRepository.save(favorite);
        }

        return product;
    }

    @Transactional
    public void removeFavorite(String email, Long productId) {

        User user = requireUser(email);

        userFavoriteRepository.deleteByUserIdAndProductId(
                user.getId(),
                productId
        );
    }

    private User requireUser(String email) {

        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario no autenticado"
                ));
    }
}
