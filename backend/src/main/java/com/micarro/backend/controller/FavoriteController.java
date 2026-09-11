package com.micarro.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.micarro.backend.entity.Product;
import com.micarro.backend.service.FavoriteService;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<Product> getFavorites(Authentication authentication) {
        return favoriteService.getFavorites(authentication.getName());
    }

    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Product addFavorite(
            @PathVariable Long productId,
            Authentication authentication) {

        return favoriteService.addFavorite(
                authentication.getName(),
                productId
        );
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(
            @PathVariable Long productId,
            Authentication authentication) {

        favoriteService.removeFavorite(
                authentication.getName(),
                productId
        );
    }
}
