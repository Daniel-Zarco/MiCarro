package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.micarro.backend.dto.PageResponse;
import com.micarro.backend.dto.ProductResponse;
import com.micarro.backend.entity.Product;
import com.micarro.backend.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    private Product product(long id, String name, String price) {

        Product product = new Product();
        product.setId(id);
        product.setExternalId("ext-" + id);
        product.setName(name);
        product.setBrand("Marca");
        product.setCategory("Categoria");
        product.setImageUrl("http://example.com/" + id + ".jpg");
        product.setFormat("1 kg");
        product.setPrice(price == null ? null : new BigDecimal(price));

        return product;
    }

    private Pageable sortedPageable(Pageable pageable) {
        Sort defaultSort = Sort.by(Sort.Order.asc("catalogOrder"));

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                defaultSort
        );
    }

    @Test
    void createProduct_returnsOwnResponse() {

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        ProductResponse response =
                productService.createProduct(product(0, "Pollo", "10"));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Pollo");
        assertThat(response.getExternalId()).isEqualTo("ext-0");
        assertThat(response.getPrice()).isEqualByComparingTo("10");
    }

    @Test
    void getProducts_mapsToOwnPageResponse() {

        Pageable pageable = PageRequest.of(0, 5);
        Pageable sortedPageable = sortedPageable(pageable);
        PageImpl<Product> page =
                new PageImpl<>(List.of(product(1, "Pollo", "10")), sortedPageable, 1);

        when(productRepository.findAll(sortedPageable)).thenReturn(page);

        PageResponse<ProductResponse> response =
                productService.getProducts(null, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Pollo");
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    void getProducts_usesSearchWhenProvided() {

        Pageable pageable = PageRequest.of(0, 5);
        Pageable sortedPageable = sortedPageable(pageable);

        when(productRepository.findByNameContainingIgnoreCase(eq("pollo"), eq(sortedPageable)))
                .thenReturn(new PageImpl<>(List.of(), sortedPageable, 0));

        productService.getProducts("  pollo  ", pageable);

        verify(productRepository).findByNameContainingIgnoreCase("pollo", sortedPageable);
    }

    @Test
    void getProductById_returnsOwnResponse() {

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product(1, "Pollo", "10")));

        Optional<ProductResponse> response =
                productService.getProductById(1L);

        assertThat(response).isPresent();
        assertThat(response.get().getName()).isEqualTo("Pollo");
    }

    @Test
    void getProductById_emptyWhenMissing() {

        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(productService.getProductById(99L)).isEmpty();
    }

    @Test
    void updateProduct_returnsUpdatedResponse() {

        Product existing = product(1, "Pollo", "10");

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        Product changes = new Product();
        changes.setName("Pollo actualizado");
        changes.setBrand("Nueva marca");
        changes.setCategory("Nueva categoria");

        Optional<ProductResponse> response =
                productService.updateProduct(1L, changes);

        assertThat(response).isPresent();
        assertThat(response.get().getName()).isEqualTo("Pollo actualizado");
        assertThat(response.get().getBrand()).isEqualTo("Nueva marca");
    }

    @Test
    void deleteProduct_returnsFalseWhenMissing() {

        when(productRepository.existsById(1L)).thenReturn(false);

        assertThat(productService.deleteProduct(1L)).isFalse();
    }
}
