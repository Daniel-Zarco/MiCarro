package com.micarro.backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.micarro.backend.dto.CategoryResponse;
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

    private static final Sort PRICE_ASC_SORT =
            Sort.by(Sort.Order.asc("price").nullsLast());

    private static final Sort PRICE_DESC_SORT =
            Sort.by(Sort.Order.desc("price").nullsLast());

    private static final Sort NAME_SORT =
            Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id"));

    public List<CategoryResponse> getCategories() {
        return productRepository.findCategorySummaries();
    }

    public PageResponse<ProductResponse> getProducts(
            String search,
            String category,
            String sortBy,
            Pageable pageable) {

        Pageable sortedPageable = withSort(pageable, sortBy);

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasCategory = category != null && !category.isBlank();

        Page<Product> page;
        if (hasSearch && hasCategory) {
            page = productRepository.findByNameContainingIgnoreCaseAndCategoryIgnoreCase(
                    search.trim(),
                    category.trim(),
                    sortedPageable
            );
        } else if (hasCategory) {
            page = productRepository.findByCategoryIgnoreCase(
                    category.trim(),
                    sortedPageable
            );
        } else if (hasSearch) {
            page = productRepository.findByNameContainingIgnoreCase(
                    search.trim(),
                    sortedPageable
            );
        } else {
            page = productRepository.findAll(sortedPageable);
        }

        return PageResponse.from(
                page,
                page.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    private Pageable withSort(Pageable pageable, String sortBy) {

        Sort sort = switch (sortBy == null ? "catalog" : sortBy) {
            case "price-asc" -> PRICE_ASC_SORT;
            case "price-desc" -> PRICE_DESC_SORT;
            case "name" -> NAME_SORT;
            default -> DEFAULT_PRODUCT_SORT;
        };

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );
    }

    private static final Sort RECENT_PRODUCT_SORT =
            Sort.by(Sort.Order.desc("firstSeenAt"));

    public PageResponse<ProductResponse> getRecentProducts(Pageable pageable) {

        Instant sixMonthsAgo = Instant.now().minus(6 * 30L, ChronoUnit.DAYS);

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                RECENT_PRODUCT_SORT
        );

        Page<Product> page = productRepository
                .findByActiveTrueAndFirstSeenAtGreaterThanEqual(
                        sixMonthsAgo,
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
