package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.micarro.backend.dto.CategoryCount;
import com.micarro.backend.dto.CategoryResponse;
import com.micarro.backend.dto.GroupResponse;
import com.micarro.backend.dto.PageResponse;
import com.micarro.backend.dto.ProductPriceChangeResponse;
import com.micarro.backend.dto.ProductResponse;
import com.micarro.backend.entity.Product;
import com.micarro.backend.repository.ProductRepository;
import com.micarro.backend.repository.ProductRepository.ProductPriceChangeRow;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MainCategoryMapper mainCategoryMapper;

    @Mock
    private VisualGroupMapper visualGroupMapper;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository,
                mainCategoryMapper,
                visualGroupMapper);
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
                productService.getProducts(null, null, null, "catalog", pageable);

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

        productService.getProducts("  pollo  ", null, null, "catalog", pageable);

        verify(productRepository).findByNameContainingIgnoreCase("pollo", sortedPageable);
    }

    @Test
    void getProducts_sortsByPriceAscendingWithNullsLastWhenRequested() {

        Pageable pageable = PageRequest.of(0, 5);

        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        productService.getProducts(null, null, null, "price-asc", pageable);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(captor.capture());

        Sort.Order order = captor.getValue().getSort().getOrderFor("price");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(order.getNullHandling()).isEqualTo(Sort.NullHandling.NULLS_LAST);
    }

    @Test
    void getProducts_sortsByPriceDescendingWithNullsLastWhenRequested() {

        Pageable pageable = PageRequest.of(0, 5);

        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        productService.getProducts(null, null, null, "price-desc", pageable);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(captor.capture());

        Sort.Order order = captor.getValue().getSort().getOrderFor("price");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(order.getNullHandling()).isEqualTo(Sort.NullHandling.NULLS_LAST);
    }

    @Test
    void getProducts_sortsByNameCaseInsensitiveWithStableSecondaryWhenRequested() {

        Pageable pageable = PageRequest.of(0, 5);

        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        productService.getProducts(null, null, null, "name", pageable);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(captor.capture());

        Sort sort = captor.getValue().getSort();
        Sort.Order nameOrder = sort.getOrderFor("name");
        Sort.Order idOrder = sort.getOrderFor("id");

        assertThat(nameOrder).isNotNull();
        assertThat(nameOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(nameOrder.isIgnoreCase()).isTrue();
        assertThat(idOrder).isNotNull();
        assertThat(idOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getProducts_sortsByNameDescendingWithStableSecondaryWhenRequested() {

        Pageable pageable = PageRequest.of(0, 5);

        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        productService.getProducts(null, null, null, "name-desc", pageable);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(captor.capture());

        Sort sort = captor.getValue().getSort();
        Sort.Order nameOrder = sort.getOrderFor("name");
        Sort.Order idOrder = sort.getOrderFor("id");

        assertThat(nameOrder).isNotNull();
        assertThat(nameOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(nameOrder.isIgnoreCase()).isTrue();
        assertThat(idOrder).isNotNull();
        assertThat(idOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getProducts_filtersByCategory() {

        Pageable pageable = PageRequest.of(0, 5);
        Pageable sortedPageable = sortedPageable(pageable);

        when(productRepository.findByMainCategoryIgnoreCase(eq("Fruta"), eq(sortedPageable)))
                .thenReturn(new PageImpl<>(List.of(), sortedPageable, 0));

        productService.getProducts(null, "  Fruta  ", null, "catalog", pageable);

        verify(productRepository).findByMainCategoryIgnoreCase("Fruta", sortedPageable);
    }

    @Test
    void getProducts_combinesSearchAndCategory() {

        Pageable pageable = PageRequest.of(0, 5);
        Pageable sortedPageable = sortedPageable(pageable);

        when(productRepository.findByNameContainingIgnoreCaseAndMainCategoryIgnoreCase(
                eq("manzana"), eq("Fruta"), eq(sortedPageable)))
                .thenReturn(new PageImpl<>(List.of(), sortedPageable, 0));

        productService.getProducts(" manzana ", "Fruta", null, "catalog", pageable);

        verify(productRepository).findByNameContainingIgnoreCaseAndMainCategoryIgnoreCase(
                "manzana", "Fruta", sortedPageable);
    }

    @Test
    void getCategories_returnsSummaries() {

        when(productRepository.findCategorySummaries())
                .thenReturn(List.of(
                        new CategoryResponse("Bebidas", 4),
                        new CategoryResponse("Frutas", 9)
                ));

        List<CategoryResponse> categories = productService.getCategories();

        assertThat(categories).hasSize(2);
        assertThat(categories.get(0).getName()).isEqualTo("Bebidas");
        assertThat(categories.get(0).getProductCount()).isEqualTo(4);
        assertThat(categories.get(1).getName()).isEqualTo("Frutas");
        assertThat(categories.get(1).getProductCount()).isEqualTo(9);
    }

    @Test
    void getVisualGroups_aggregatesCountsByGroupInTreeOrder() {

        when(productRepository.countByCategoryInMainCategory("Higiene y cuidado personal"))
                .thenReturn(List.of(
                        new CategoryCount("Champú", 3),
                        new CategoryCount("Crema de cara", 5),
                        new CategoryCount("Maquillaje compacto", 2),
                        new CategoryCount("Nueva desconocida", 1)
                ));
        when(visualGroupMapper.visualGroup("Champú")).thenReturn("Cabello");
        when(visualGroupMapper.visualGroup("Crema de cara")).thenReturn("Cuidado facial");
        when(visualGroupMapper.visualGroup("Maquillaje compacto")).thenReturn("Maquillaje");
        when(visualGroupMapper.visualGroup("Nueva desconocida")).thenReturn(null);
        when(visualGroupMapper.groupsOf("Higiene y cuidado personal"))
                .thenReturn(List.of("Cabello", "Cuidado facial", "Maquillaje"));

        List<GroupResponse> groups =
                productService.getVisualGroups("Higiene y cuidado personal");

        assertThat(groups).hasSize(4);
        assertThat(groups.get(0).getName()).isEqualTo("Cabello");
        assertThat(groups.get(0).getProductCount()).isEqualTo(3);
        assertThat(groups.get(1).getName()).isEqualTo("Cuidado facial");
        assertThat(groups.get(1).getProductCount()).isEqualTo(5);
        assertThat(groups.get(2).getName()).isEqualTo("Maquillaje");
        assertThat(groups.get(2).getProductCount()).isEqualTo(2);
        assertThat(groups.get(3).getName()).isEqualTo("Otros");
        assertThat(groups.get(3).getProductCount()).isEqualTo(1);
    }

    @Test
    void getProducts_filtersByGroupCategories() {

        Pageable pageable = PageRequest.of(0, 5);
        Pageable sortedPageable = sortedPageable(pageable);

        when(visualGroupMapper.categoriesOf("Fruta", "Fruta"))
                .thenReturn(List.of("Manzana y pera", "Melocotón"));
        when(productRepository.findByMainCategoryIgnoreCaseAndCategoryIn(
                eq("Fruta"), eq(List.of("Manzana y pera", "Melocotón")), eq(sortedPageable)))
                .thenReturn(new PageImpl<>(List.of(), sortedPageable, 0));

        productService.getProducts(null, "Fruta", "Fruta", "catalog", pageable);

        verify(productRepository).findByMainCategoryIgnoreCaseAndCategoryIn(
                "Fruta", List.of("Manzana y pera", "Melocotón"), sortedPageable);
    }

    @Test
    void getProducts_combinesSearchAndGroup() {

        Pageable pageable = PageRequest.of(0, 5);
        Pageable sortedPageable = sortedPageable(pageable);

        when(visualGroupMapper.categoriesOf("Fruta", "Fruta"))
                .thenReturn(List.of("Manzana y pera"));
        when(productRepository.findByNameContainingIgnoreCaseAndMainCategoryIgnoreCaseAndCategoryIn(
                eq("manzana"), eq("Fruta"), eq(List.of("Manzana y pera")), eq(sortedPageable)))
                .thenReturn(new PageImpl<>(List.of(), sortedPageable, 0));

        productService.getProducts(" manzana ", "Fruta", "Fruta", "catalog", pageable);

        verify(productRepository).findByNameContainingIgnoreCaseAndMainCategoryIgnoreCaseAndCategoryIn(
                "manzana", "Fruta", List.of("Manzana y pera"), sortedPageable);
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

    @Test
    void getRecentProducts_filtersByLastSixMonthsAndSortsByFirstSeenAtDescThenIdAsc() {

        Pageable pageable = PageRequest.of(0, 10);
        Sort recentSort = Sort.by(
                Sort.Order.desc("firstSeenAt"),
                Sort.Order.asc("id")
        );
        Pageable sortedPageable = PageRequest.of(0, 10, recentSort);

        Instant sixMonthsAgo = Instant.now().minus(6 * 30L, ChronoUnit.DAYS);

        PageImpl<Product> page =
                new PageImpl<>(List.of(product(1, "Nuevo", "5")), sortedPageable, 1);

        when(productRepository
                .findByActiveTrueAndFirstSeenAtGreaterThanEqual(
                        org.mockito.ArgumentMatchers.argThat(
                                instant -> instant.isAfter(sixMonthsAgo.minusSeconds(60))
                                        && instant.isBefore(sixMonthsAgo.plusSeconds(60))
                        ),
                        org.mockito.ArgumentMatchers.eq(sortedPageable)
                )
        ).thenReturn(page);

        PageResponse<ProductResponse> response =
                productService.getRecentProducts(pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Nuevo");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    private ProductPriceChangeRow row(long id, String current, String previous) {

        return new ProductPriceChangeRow() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getExternalId() {
                return "ext-" + id;
            }

            @Override
            public String getName() {
                return "Producto " + id;
            }

            @Override
            public String getBrand() {
                return "Marca";
            }

            @Override
            public String getCategory() {
                return "Categoria";
            }

            @Override
            public String getImageUrl() {
                return "http://example.com/" + id + ".jpg";
            }

            @Override
            public String getFormat() {
                return "1 kg";
            }

            @Override
            public BigDecimal getCurrentPrice() {
                return new BigDecimal(current);
            }

            @Override
            public BigDecimal getPreviousPrice() {
                return new BigDecimal(previous);
            }
        };
    }

    @Test
    void getNewProducts_returnsProductsSeenInLast30Days() {

        Product nuevo = product(1, "Nuevo", "5");
        nuevo.setFirstSeenAt(Instant.now());

        Pageable pageable = PageRequest.of(0, 10);
        Pageable unsorted = PageRequest.of(0, 10);

        when(productRepository.findNewProducts(
                any(), org.mockito.ArgumentMatchers.eq(unsorted)))
                .thenReturn(new PageImpl<>(List.of(nuevo), unsorted, 1));

        PageResponse<ProductPriceChangeResponse> response =
                productService.getNewProducts(pageable);

        ProductPriceChangeResponse item = response.getContent().get(0);
        assertThat(item.getCurrentPrice()).isEqualByComparingTo("5");
        assertThat(item.getPreviousPrice()).isNull();
        assertThat(item.getDifference()).isNull();
        assertThat(item.getDifferencePercent()).isNull();
        assertThat(item.getFirstSeenAt()).isNotNull();
    }

    @Test
    void getPriceDrops_mapsDifferenceAndPercentPreservingRepoOrder() {

        Pageable pageable = PageRequest.of(0, 10);
        Pageable unsorted = PageRequest.of(0, 10);

        // Orden que ya viene del SQL: mayor bajada primero.
        PageImpl<ProductPriceChangeRow> page = new PageImpl<>(
                List.of(
                        row(1, "5", "10"),   // -50%
                        row(2, "7", "10")    // -30%
                ),
                unsorted,
                2
        );

        when(productRepository.findPriceDrops(any(), org.mockito.ArgumentMatchers.eq(unsorted)))
                .thenReturn(page);

        PageResponse<ProductPriceChangeResponse> response =
                productService.getPriceDrops(pageable);

        assertThat(response.getContent()).hasSize(2);

        ProductPriceChangeResponse first = response.getContent().get(0);
        assertThat(first.getCurrentPrice()).isEqualByComparingTo("5");
        assertThat(first.getPreviousPrice()).isEqualByComparingTo("10");
        assertThat(first.getDifference()).isEqualByComparingTo("-5");
        assertThat(first.getDifferencePercent()).isEqualByComparingTo("-50.00");

        ProductPriceChangeResponse second = response.getContent().get(1);
        assertThat(second.getDifferencePercent()).isEqualByComparingTo("-30.00");
    }

    @Test
    void getPriceRaises_mapsRiseAndPercent() {

        Pageable pageable = PageRequest.of(0, 10);
        Pageable unsorted = PageRequest.of(0, 10);

        PageImpl<ProductPriceChangeRow> page = new PageImpl<>(
                List.of(row(3, "12", "10")),   // +20%
                unsorted,
                1
        );

        when(productRepository.findPriceRaises(any(), org.mockito.ArgumentMatchers.eq(unsorted)))
                .thenReturn(page);

        PageResponse<ProductPriceChangeResponse> response =
                productService.getPriceRaises(pageable);

        ProductPriceChangeResponse item = response.getContent().get(0);
        assertThat(item.getDifference()).isEqualByComparingTo("2");
        assertThat(item.getDifferencePercent()).isEqualByComparingTo("20.00");
    }

    @Test
    void priceChanges_usesCutoffThirtyDaysAgo() {

        ProductPriceChangeRow drop = row(4, "6", "9");
        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.findPriceDrops(
                org.mockito.ArgumentMatchers.argThat(
                        instant -> {
                            Instant expected = Instant.now().minus(30, ChronoUnit.DAYS);
                            return instant.isAfter(expected.minusSeconds(60))
                                    && instant.isBefore(expected.plusSeconds(60));
                        }
                ),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(new PageImpl<>(List.of(drop), PageRequest.of(0, 10), 1));

        PageResponse<ProductPriceChangeResponse> response =
                productService.getPriceDrops(pageable);

        assertThat(response.getContent()).hasSize(1);
    }
}
