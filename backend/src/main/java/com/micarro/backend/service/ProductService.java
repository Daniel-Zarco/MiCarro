package com.micarro.backend.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.micarro.backend.dto.PageResponse;
import com.micarro.backend.dto.ProductResponse;
import com.micarro.backend.entity.Product;
import com.micarro.backend.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse createProduct(Product product) {
        return toResponse(productRepository.save(product));
    }

    private static final Sort DEFAULT_PRODUCT_SORT =
            Sort.by(Sort.Order.asc("catalogOrder"));

    public PageResponse<ProductResponse> getProducts(
            String search,
            Pageable pageable) {

        Pageable sortedPageable = withDefaultSort(pageable);

        Page<Product> page = (search == null || search.isBlank())
                ? productRepository.findAll(sortedPageable)
                : productRepository.findByNameContainingIgnoreCase(
                        search.trim(),
                        sortedPageable
                );

        return PageResponse.from(
                page,
                page.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    private Pageable withDefaultSort(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                DEFAULT_PRODUCT_SORT
        );
    }

    public Optional<ProductResponse> getProductById(Long id) {
        return productRepository.findById(id)
                .filter(Product::isActive)
                .map(this::toResponse);
    }

    public Optional<ProductResponse> updateProduct(
            Long id,
            Product productDetails) {

        return productRepository.findById(id)
                .map(product -> {
                    product.setName(productDetails.getName());
                    product.setBrand(productDetails.getBrand());
                    product.setCategory(productDetails.getCategory());

                    return toResponse(productRepository.save(product));
                });
    }

    public boolean deleteProduct(Long id) {

        if (!productRepository.existsById(id)) {
            return false;
        }

        productRepository.deleteById(id);

        return true;
    }

    private ProductResponse toResponse(Product product) {

        return new ProductResponse(
                product.getId(),
                product.getExternalId(),
                product.getName(),
                product.getBrand(),
                product.getCategory(),
                product.getImageUrl(),
                product.getFormat(),
                product.getPrice()
        );
    }
}
