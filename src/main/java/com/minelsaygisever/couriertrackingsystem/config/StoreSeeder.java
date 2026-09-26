package com.minelsaygisever.couriertrackingsystem.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minelsaygisever.couriertrackingsystem.domain.Store;
import com.minelsaygisever.couriertrackingsystem.exception.StoreInitializationException;
import com.minelsaygisever.couriertrackingsystem.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StoreSeeder implements CommandLineRunner {

    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        if (storeRepository.count() == 0) {
            log.info("No stores found in DB. Seeding from stores.json...");
            try {
                ClassPathResource resource = new ClassPathResource("stores.json");
                InputStream inputStream = resource.getInputStream();

                List<Store> stores = objectMapper.readValue(inputStream, new TypeReference<List<Store>>() {});

                storeRepository.saveAll(stores);

                log.info("Successfully seeded {} stores into Database.", stores.size());
            } catch (IOException e) {
                log.error("Failed to seed stores!", e);
                throw new StoreInitializationException("Failed to seed stores from JSON file", e);
            }
        } else {
            log.info("Stores already exist in DB. Skipping seed.");
        }
    }
}
