package com.micarro.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.micarro.backend.dto.SyncResult;
import com.micarro.backend.external.mercadona.MercadonaClient;
import com.micarro.backend.service.ProductSyncService;

@ExtendWith(MockitoExtension.class)
class MercadonaControllerTest {

    @Mock
    private MercadonaClient mercadonaClient;

    @Mock
    private ProductSyncService productSyncService;

    @Test
    void importProductsReturnsSyncResult() {

        SyncResult expected = new SyncResult(3, 2, 1, 0, 0, 0);

        when(productSyncService.sync()).thenReturn(expected);

        MercadonaController controller =
                new MercadonaController(mercadonaClient, productSyncService);

        assertThat(controller.importProducts()).isSameAs(expected);
    }
}
