package com.minelsaygisever.couriertrackingsystem.config;

import com.minelsaygisever.couriertrackingsystem.domain.Store;
import com.minelsaygisever.couriertrackingsystem.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StoreSeederIntegrationTest {

    @Autowired
    private StoreRepository storeRepository;

    @Test
    @DisplayName("Should automatically seed stores from JSON file on startup")
    void shouldSeedStoresOnStartup() {
        List<Store> stores = storeRepository.findAll();

        assertThat(stores).hasSize(5);

        boolean grandCentralExists = stores.stream()
                .anyMatch(store -> store.getName().equals("Grand Central Supermarket") &&
                        store.getLatitude() == 40.9923307);

        assertThat(grandCentralExists).isTrue();
    }
}