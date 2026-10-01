package com.micarro.backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.micarro.backend.dto.CategoryCount;
import com.micarro.backend.dto.CategoryResponse;
import com.micarro.backend.dto.GroupResponse;
import com.micarro.backend.dto.PageResponse;
import com.micarro.backend.dto.ProductPriceChangeResponse;
import com.micarro.backend.dto.ProductResponse;
import com.micarro.backend.entity.Product;
import com.micarro.backend.repository.ProductRepository;
import com.micarro.backend.repository.ProductRepository.ProductPriceChangeRow;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final MainCategoryMapper mainCategoryMapper;
    private final VisualGroupMapper visualGroupMapper;

    public ProductService(
            ProductRepository productRepository,
            MainCategoryMapper mainCategoryMapper,
            VisualGroupMapper visualGroupMapper) {

        this.productRepository = productRepository;
        this.mainCategoryMapper = mainCategoryMapper;
        this.visualGroupMapper = visualGroupMapper;
    }

    public ProductResponse createProduct(Product product) {
        product.setMainCategory(
                mainCategoryMapper.map(product.getCategory())
        );
        return toResponse(productRepository.save(product));
    }

    private static final Sort DEFAULT_PRODUCT_SORT =
            Sort.by(
                    Sort.Order.asc("catalogOrder"),
                    Sort.Order.asc("id")
            );

    private static final Sort PRICE_ASC_SORT =
            Sort.by(
                    Sort.Order.asc("price").nullsLast(),
                    Sort.Order.asc("catalogOrder"),
                    Sort.Order.asc("id")
            );

    private static final Sort PRICE_DESC_SORT =
            Sort.by(
                    Sort.Order.desc("price").nullsLast(),
                    Sort.Order.asc("catalogOrder"),
                    Sort.Order.asc("id")
            );

    private static final Sort NAME_SORT =
            Sort.by(
                    Sort.Order.asc("name").ignoreCase(),
                    Sort.Order.asc("catalogOrder"),
                    Sort.Order.asc("id")
            );

    private static final Sort NAME_DESC_SORT =
            Sort.by(
                    Sort.Order.desc("name").ignoreCase(),
                    Sort.Order.asc("catalogOrder"),
                    Sort.Order.asc("id")
            );

    public List<CategoryResponse> getCategories() {
        return productRepository.findCategorySummaries();
    }

    public List<GroupResponse> getVisualGroups(String mainCategory) {

        Map<String, Long> countsByGroup = new LinkedHashMap<>();

        for (CategoryCount count : productRepository.countByCategoryInMainCategory(mainCategory)) {
            String group = visualGroupMapper.visualGroup(count.category());
            if (group == null) {
                group = VisualGroupMapper.FALLBACK_GROUP;
            }
            countsByGroup.merge(group, count.count(), Long::sum);
        }

        List<GroupResponse> result = new ArrayList<>();

        for (String group : visualGroupMapper.groupsOf(mainCategory)) {
            Long count = countsByGroup.get(group);
            if (count != null) {
                result.add(new GroupResponse(group, count));
            }
        }

        Long fallbackCount = countsByGroup.get(VisualGroupMapper.FALLBACK_GROUP);
        if (fallbackCount != null) {
            result.add(new GroupResponse(VisualGroupMapper.FALLBACK_GROUP, fallbackCount));
        }

        return result;
    }

    public PageResponse<ProductResponse> getProducts(
            String search,
            String category,
            String group,
            String sortBy,
            Pageable pageable) {

        Pageable sortedPageable = withSort(pageable, sortBy);

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasGroup = group != null && !group.isBlank();

        Page<Product> page;
        if (hasCategory && hasGroup) {
            List<String> categories = groupCategories(category.trim(), group.trim());
            if (categories.isEmpty()) {
                page = Page.empty(sortedPageable);
            } else if (hasSearch) {
                page = productRepository.findByNameContainingIgnoreCaseAndMainCategoryIgnoreCaseAndCategoryIn(
                        search.trim(),
                        category.trim(),
                        categories,
                        sortedPageable
                );
            } else {
                page = productRepository.findByMainCategoryIgnoreCaseAndCategoryIn(
                        category.trim(),
                        categories,
                        sortedPageable
                );
            }
        } else if (hasSearch && hasCategory) {
            page = productRepository.findByNameContainingIgnoreCaseAndMainCategoryIgnoreCase(
                    search.trim(),
                    category.trim(),
                    sortedPageable
            );
        } else if (hasCategory) {
            page = productRepository.findByMainCategoryIgnoreCase(
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

    private List<String> groupCategories(String mainCategory, String group) {

        if (VisualGroupMapper.FALLBACK_GROUP.equals(group)) {
            return productRepository.findDistinctCategoryByMainCategory(mainCategory)
                    .stream()
                    .filter(category -> visualGroupMapper.visualGroup(category) == null)
                    .toList();
        }

        return visualGroupMapper.categoriesOf(mainCategory, group);
    }

    private Pageable withSort(Pageable pageable, String sortBy) {

        Sort sort = switch (sortBy == null ? "catalog" : sortBy) {
            case "price-asc" -> PRICE_ASC_SORT;
            case "price-desc" -> PRICE_DESC_SORT;
            case "name" -> NAME_SORT;
            case "name-desc" -> NAME_DESC_SORT;
            default -> DEFAULT_PRODUCT_SORT;
        };

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );
    }

    private static final Sort RECENT_PRODUCT_SORT =
            Sort.by(
                    Sort.Order.desc("firstSeenAt"),
                    Sort.Order.asc("id")
            );

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

    private static final int NEW_WINDOW_DAYS = 30;
    private static final int PRICE_WINDOW_DAYS = 30;

    /*
     * Nuevos: productos activos detectados en los últimos 30 días por
     * firstSeenAt, ordenados por firstSeenAt desc, y sin referencia histórica
     * equivalente anterior (evita falsos "nuevos" por re-creación o cambio de
     * externalId).
     */
    public PageResponse<ProductPriceChangeResponse> getNewProducts(Pageable pageable) {

        Instant since = Instant.now().minus(NEW_WINDOW_DAYS, ChronoUnit.DAYS);

        Pageable unsorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<Product> page = productRepository.findNewProducts(since, unsorted);

        return PageResponse.from(
                page,
                page.getContent()
                        .stream()
                        .map(ProductPriceChangeResponse::forNewProduct)
                        .toList()
        );
    }

    public PageResponse<ProductPriceChangeResponse> getPriceDrops(Pageable pageable) {
        return priceChanges(pageable, true);
    }

    public PageResponse<ProductPriceChangeResponse> getPriceRaises(Pageable pageable) {
        return priceChanges(pageable, false);
    }

    private PageResponse<ProductPriceChangeResponse> priceChanges(
            Pageable pageable,
            boolean drops) {

        Instant cutoff = Instant.now().minus(PRICE_WINDOW_DAYS, ChronoUnit.DAYS);

        // Sin sort adicional: el orden (mayor % primero) lo define la consulta SQL.
        Pageable unsorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<ProductPriceChangeRow> page = drops
                ? productRepository.findPriceDrops(cutoff, unsorted)
                : productRepository.findPriceRaises(cutoff, unsorted);

        return PageResponse.from(
                page,
                page.getContent()
                        .stream()
                        .map(ProductPriceChangeResponse::fromRow)
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
