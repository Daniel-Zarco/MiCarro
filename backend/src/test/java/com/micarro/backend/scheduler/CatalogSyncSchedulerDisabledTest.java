package com.micarro.backend.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class CatalogSyncSchedulerDisabledTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void schedulerIsDisabledByDefault() {

        assertThat(context.getBeanNamesForType(CatalogSyncScheduler.class))
                .isEmpty();
    }
}
