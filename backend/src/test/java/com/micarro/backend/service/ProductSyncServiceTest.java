package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.micarro.backend.dto.SyncResult;
import com.micarro.backend.entity.Product;
import com.micarro.backend.provider.ProductProvider;
import com.micarro.backend.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductSyncServiceTest {

    private static final String SOURCE = "MERCADONA";

    @Mock
    private ProductProvider productProvider;

    @Mock
    private ProductRepository productRepository;

    private ProductSyncService productSyncService;

    @BeforeEach
    void setUp() {
        productSyncService = new ProductSyncService(productProvider, productRepository);
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
}
