package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.micarro.backend.dto.SyncResult;
import com.micarro.backend.entity.Product;
import com.micarro.backend.entity.ProductPriceHistory;
import com.micarro.backend.provider.ProductProvider;
import com.micarro.backend.repository.ProductPriceHistoryRepository;
import com.micarro.backend.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductSyncServiceTest {

    private static final String SOURCE = "MERCADONA";

    @Mock
    private ProductProvider productProvider;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MainCategoryMapper mainCategoryMapper;

    @Mock
    private ProductPriceHistoryRepository priceHistoryRepository;

    private ProductSyncService productSyncService;

    @BeforeEach
    void setUp() {
        productSyncService = new ProductSyncService(
                productProvider,
                productRepository,
                mainCategoryMapper,
                priceHistoryRepository);
    }

    private Product incoming(
            String externalId,
            String name,
            String category,
            String imageUrl,
            String format,
            String price) {

        Product product = new Product();
        product.setExternalId(externalId);
        product.setName(name);
        product.setCategory(category);
        product.setImageUrl(imageUrl);
        product.setFormat(format);
        product.setPrice(price == null ? null : new BigDecimal(price));
        return product;
    }

    private Product existing(
            long id,
            String externalId,
            String name,
            String category,
            String imageUrl,
            String format,
            String price,
            boolean active) {

        Product product = incoming(externalId, name, category, imageUrl, format, price);
        product.setId(id);
        product.setSource(SOURCE);
        product.setActive(active);
        product.setCatalogOrder(0);
        product.setLastSyncedAt(Instant.now());
        return product;
    }

    @Test
    void sync_createsNewProducts() {

        List<Product> saved = new ArrayList<>();

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts())
                .thenReturn(List.of(incoming("A", "Pollo", "Carnes", "img", "1kg", "10")));
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of());
        when(productRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    saved.addAll(invocation.getArgument(0));
                    return invocation.getArgument(0);
                });

        SyncResult result = productSyncService.sync();

        assertThat(result.getReceived()).isEqualTo(1);
        assertThat(result.getCreated()).isEqualTo(1);
        assertThat(result.getUpdated()).isZero();
        assertThat(result.getErrors()).isZero();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getSource()).isEqualTo(SOURCE);
        assertThat(saved.get(0).isActive()).isTrue();
        assertThat(saved.get(0).getLastSyncedAt()).isNotNull();
        assertThat(saved.get(0).getFirstSeenAt()).isNotNull();
        assertThat(saved.get(0).getCatalogOrder()).isZero();
    }

    @Test
    void sync_assignsCatalogOrderBasedOnProviderSequence() {

        List<Product> saved = new ArrayList<>();

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts())
                .thenReturn(List.of(
                        incoming("C", "Tercero", "Cat", "img", "1kg", "3"),
                        incoming("A", "Primero", "Cat", "img", "1kg", "1"),
                        incoming("B", "Segundo", "Cat", "img", "1kg", "2")
                ));
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of());
        when(productRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    saved.addAll(invocation.getArgument(0));
                    return invocation.getArgument(0);
                });

        productSyncService.sync();

        assertThat(saved).hasSize(3);
        assertThat(saved.get(0).getExternalId()).isEqualTo("C");
        assertThat(saved.get(0).getCatalogOrder()).isZero();
        assertThat(saved.get(1).getExternalId()).isEqualTo("A");
        assertThat(saved.get(1).getCatalogOrder()).isEqualTo(1);
        assertThat(saved.get(2).getExternalId()).isEqualTo("B");
        assertThat(saved.get(2).getCatalogOrder()).isEqualTo(2);
    }

    @Test
    void sync_updatesChangedFieldsAndPrice() {

        Product stored = existing(1L, "A", "Pollo", "Carnes", "img", "1kg", "10", true);

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts())
                .thenReturn(List.of(incoming("A", "Pollo fresco", "Carnes", "img2", "1kg", "12")));
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of(stored));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        SyncResult result = productSyncService.sync();

        assertThat(result.getUpdated()).isEqualTo(1);
        assertThat(result.getCreated()).isZero();
        assertThat(stored.getName()).isEqualTo("Pollo fresco");
        assertThat(stored.getImageUrl()).isEqualTo("img2");
        assertThat(stored.getPrice()).isEqualByComparingTo("12");
        assertThat(stored.getExternalId()).isEqualTo("A");
        assertThat(stored.getCatalogOrder()).isZero();
    }

    @Test
    void sync_updatesCatalogOrderForExistingProducts() {

        Product stored = existing(1L, "A", "Pollo", "Carnes", "img", "1kg", "10", true);
        stored.setCatalogOrder(5);

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts())
                .thenReturn(List.of(
                        incoming("B", "Otro", "Carnes", "img", "1kg", "5"),
                        incoming("A", "Pollo", "Carnes", "img", "1kg", "10")
                ));
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of(stored));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        SyncResult result = productSyncService.sync();

        assertThat(result.getCreated()).isEqualTo(1);
        assertThat(result.getUpdated()).isEqualTo(1);
        assertThat(result.getUnchanged()).isZero();
        assertThat(stored.getCatalogOrder()).isEqualTo(1);
    }

    @Test
    void sync_countsUnchangedProducts() {

        Product stored = existing(1L, "A", "Pollo", "Carnes", "img", "1kg", "10", true);

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts())
                .thenReturn(List.of(incoming("A", "Pollo", "Carnes", "img", "1kg", "10.00")));
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of(stored));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        SyncResult result = productSyncService.sync();

        assertThat(result.getUnchanged()).isEqualTo(1);
        assertThat(result.getUpdated()).isZero();
    }

    @Test
    void sync_preservesFirstSeenAtForExistingProducts() {

        Instant originalFirstSeen = Instant.parse("2026-01-01T00:00:00Z");
        Product stored = existing(1L, "A", "Pollo", "Carnes", "img", "1kg", "10", true);
        stored.setFirstSeenAt(originalFirstSeen);

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts())
                .thenReturn(List.of(incoming("A", "Pollo fresco", "Carnes", "img2", "1kg", "12")));
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of(stored));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        SyncResult result = productSyncService.sync();

        assertThat(result.getUpdated()).isEqualTo(1);
        assertThat(stored.getFirstSeenAt()).isEqualTo(originalFirstSeen);
    }

    @Test
    void sync_doesNotSetFirstSeenAtWhenReactivating() {

        Product stored = existing(1L, "A", "Pollo", "Carnes", "img", "1kg", "10", false);
        stored.setFirstSeenAt(null);

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts())
                .thenReturn(List.of(incoming("A", "Pollo", "Carnes", "img", "1kg", "10")));
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of(stored));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        SyncResult result = productSyncService.sync();

        assertThat(stored.isActive()).isTrue();
        assertThat(stored.getFirstSeenAt()).isNull();
        assertThat(result.getUnchanged()).isEqualTo(1);
    }

    @Test
    void sync_deactivatesMissingProducts() {

        Product stored = existing(1L, "A", "Pollo", "Carnes", "img", "1kg", "10", true);

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts()).thenReturn(List.of());
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of(stored));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        SyncResult result = productSyncService.sync();

        assertThat(result.getDeactivated()).isEqualTo(1);
        assertThat(stored.isActive()).isFalse();
    }

    @Test
    void sync_reactivatesReappearingProducts() {

        Product stored = existing(1L, "A", "Pollo", "Carnes", "img", "1kg", "10", false);

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts())
                .thenReturn(List.of(incoming("A", "Pollo", "Carnes", "img", "1kg", "10")));
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of(stored));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        SyncResult result = productSyncService.sync();

        assertThat(stored.isActive()).isTrue();
        assertThat(result.getDeactivated()).isZero();
        assertThat(result.getUnchanged()).isEqualTo(1);
    }

    @Test
    void sync_isIdempotent() {

        List<Product> database = new ArrayList<>();

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts())
                .thenReturn(List.of(incoming("A", "Pollo", "Carnes", "img", "1kg", "10")));
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenAnswer(invocation -> new ArrayList<>(database));
        when(productRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    List<Product> batch = invocation.getArgument(0);
                    for (Product product : batch) {
                        database.removeIf(existing ->
                                Objects.equals(existing.getExternalId(), product.getExternalId()));
                        database.add(product);
                    }
                    return batch;
                });

        SyncResult first = productSyncService.sync();
        SyncResult second = productSyncService.sync();

        assertThat(first.getCreated()).isEqualTo(1);
        assertThat(second.getCreated()).isZero();
        assertThat(second.getUpdated()).isZero();
        assertThat(second.getUnchanged()).isEqualTo(1);
        assertThat(second.getDeactivated()).isZero();
        assertThat(database).hasSize(1);
    }

    @Test
    void sync_setsMainCategoryForNewAndExistingProducts() {

        List<Product> saved = new ArrayList<>();

        Product stored = existing(1L, "A", "Pollo", "Carnes", "img", "1kg", "10", true);

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts())
                .thenReturn(List.of(
                        incoming("A", "Pollo", "Carnes", "img", "1kg", "10"),
                        incoming("B", "Manzanas", "Frutas", "img", "1kg", "2")
                ));
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of(stored));
        when(mainCategoryMapper.map("Carnes")).thenReturn("Carne");
        when(mainCategoryMapper.map("Frutas")).thenReturn("Frutas y verduras");
        when(productRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    saved.addAll(invocation.getArgument(0));
                    return invocation.getArgument(0);
                });

        SyncResult result = productSyncService.sync();

        assertThat(result.getCreated()).isEqualTo(1);
        assertThat(result.getUpdated()).isEqualTo(1);
        assertThat(stored.getMainCategory()).isEqualTo("Carne");
        assertThat(saved)
                .filteredOn(product -> "B".equals(product.getExternalId()))
                .singleElement()
                .extracting(Product::getMainCategory)
                .isEqualTo("Frutas y verduras");
    }

    @Test
    void sync_doesNotQueryPerProduct() {

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts())
                .thenReturn(List.of(incoming("A", "Uno", null, null, null, "1")));
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of());
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        productSyncService.sync();

        verify(productRepository, never()).findByExternalId(anyString());
        verify(productRepository, times(1)).findBySourceIncludingInactive(SOURCE);
    }

    @Test
    void sync_handlesProblematicProductWithoutBreakingOthers() {

        Product valid = incoming("A", "Pollo", "Carnes", "img", "1kg", "10");
        Product withoutExternalId = incoming(null, "Sin id", "Carnes", "img", "1kg", "5");

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts())
                .thenReturn(List.of(valid, withoutExternalId));
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of());
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        SyncResult result = productSyncService.sync();

        assertThat(result.getReceived()).isEqualTo(2);
        assertThat(result.getCreated()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(1);
    }

    @Test
    void sync_createsInitialPriceHistoryForNewProducts() {

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts())
                .thenReturn(List.of(incoming("A", "Pollo", "Carnes", "img", "1kg", "10")));
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of());
        when(productRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    List<Product> batch = invocation.getArgument(0);
                    for (int i = 0; i < batch.size(); i++) {
                        batch.get(i).setId(i + 1L);
                    }
                    return batch;
                });

        productSyncService.sync();

        ArgumentCaptor<List<ProductPriceHistory>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(priceHistoryRepository).saveAll(captor.capture());

        List<ProductPriceHistory> history = captor.getValue();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getProductId()).isEqualTo(1L);
        assertThat(history.get(0).getPrice()).isEqualByComparingTo("10");
        assertThat(history.get(0).getRecordedAt()).isNotNull();
    }

    @Test
    void sync_withoutChange_doesNotRecordPriceHistory() {

        Product stored = existing(1L, "A", "Pollo", "Carnes", "img", "1kg", "10", true);

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts())
                .thenReturn(List.of(incoming("A", "Pollo", "Carnes", "img", "1kg", "10")));
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of(stored));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        productSyncService.sync();

        verify(priceHistoryRepository, never()).saveAll(any());
    }

    @Test
    void sync_priceChange_recordsExactlyOneRow() {

        Product stored = existing(1L, "A", "Pollo", "Carnes", "img", "1kg", "10", true);

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productProvider.getProducts())
                .thenReturn(List.of(incoming("A", "Pollo", "Carnes", "img", "1kg", "12")));
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of(stored));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        productSyncService.sync();

        ArgumentCaptor<List<ProductPriceHistory>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(priceHistoryRepository).saveAll(captor.capture());

        List<ProductPriceHistory> history = captor.getValue();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getProductId()).isEqualTo(1L);
        assertThat(history.get(0).getPrice()).isEqualByComparingTo("12");
    }

    @Test
    void sync_multiplePriceChanges_recordsOneRowPerChange() {

        Product stored = existing(1L, "A", "Pollo", "Carnes", "img", "1kg", "10", true);

        when(productProvider.getSource()).thenReturn(SOURCE);
        when(productRepository.findBySourceIncludingInactive(SOURCE))
                .thenReturn(List.of(stored));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        when(productProvider.getProducts())
                .thenReturn(List.of(incoming("A", "Pollo", "Carnes", "img", "1kg", "12")));
        productSyncService.sync(); // 10 -> 12 (fila 1)

        when(productProvider.getProducts())
                .thenReturn(List.of(incoming("A", "Pollo", "Carnes", "img", "1kg", "9")));
        productSyncService.sync(); // 12 -> 9 (fila 2)

        ArgumentCaptor<List<ProductPriceHistory>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(priceHistoryRepository, times(2)).saveAll(captor.capture());

        List<List<ProductPriceHistory>> allHistory = captor.getAllValues();
        assertThat(allHistory).hasSize(2);
        assertThat(allHistory.get(0).get(0).getPrice()).isEqualByComparingTo("12");
        assertThat(allHistory.get(1).get(0).getPrice()).isEqualByComparingTo("9");
    }
}
