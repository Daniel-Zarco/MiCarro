package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.micarro.backend.entity.Product;
import com.micarro.backend.entity.User;
import com.micarro.backend.repository.ProductRepository;
import com.micarro.backend.repository.UserFavoriteRepository;
import com.micarro.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserFavoriteRepository userFavoriteRepository;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(
                userRepository,
                productRepository,
                userFavoriteRepository
        );
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setName("Dani");
        user.setEmail("dani@example.com");
        return user;
    }

    private Product product(long id) {
        Product product = new Product();
        product.setId(id);
        product.setName("Producto " + id);
        return product;
    }

    @Test
    void getFavorites_returnsProductsOfAuthenticatedUser() {

        Product product = product(10);

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(userFavoriteRepository.findProductsByUserId(1L))
                .thenReturn(List.of(product));

        assertThat(favoriteService.getFavorites("dani@example.com"))
                .containsExactly(product);
    }

    @Test
    void getFavorites_throwsUnauthorizedForUnknownUser() {

        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> favoriteService.getFavorites("nobody@example.com"))
                .satisfies(exception -> assertThat(exception.getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void addFavorite_savesWhenItDoesNotExist() {

        Product product = product(10);

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));
        when(userFavoriteRepository.existsByUserIdAndProductId(1L, 10L))
                .thenReturn(false);

        Product result = favoriteService.addFavorite("dani@example.com", 10L);

        assertThat(result).isEqualTo(product);
        verify(userFavoriteRepository).save(any());
    }

    @Test
    void addFavorite_doesNotDuplicateWhenItAlreadyExists() {

        Product product = product(10);

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));
        when(userFavoriteRepository.existsByUserIdAndProductId(1L, 10L))
                .thenReturn(true);

        favoriteService.addFavorite("dani@example.com", 10L);

        verify(userFavoriteRepository, never()).save(any());
    }

    @Test
    void addFavorite_throwsNotFoundWhenProductDoesNotExist() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> favoriteService.addFavorite("dani@example.com", 999L))
                .satisfies(exception -> assertThat(exception.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void removeFavorite_deletesForAuthenticatedUser() {

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user()));

        favoriteService.removeFavorite("dani@example.com", 10L);

        verify(userFavoriteRepository).deleteByUserIdAndProductId(1L, 10L);
    }
}
