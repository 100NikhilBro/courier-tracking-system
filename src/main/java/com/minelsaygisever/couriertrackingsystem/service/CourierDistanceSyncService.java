package com.minelsaygisever.couriertrackingsystem.service;

import com.minelsaygisever.couriertrackingsystem.repository.CourierProfileRepository;
import com.minelsaygisever.couriertrackingsystem.util.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierDistanceSyncService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CourierProfileRepository courierProfileRepository;
    private final TransactionTemplate transactionTemplate;

    // Temporary processing switch to prevent race condition
    private static final String TEMP_PROCESSING_KEY = "courier:distance:processing";

    /**
     * Runs every minute (60,000ms).
     * Retrieves accumulated distances from Redis and writes them to the database.
     */
    @Scheduled(fixedRate = 60000)
    public void syncDistancesToDb() {
        // CRASH RECOVERY
        if (Boolean.TRUE.equals(redisTemplate.hasKey(TEMP_PROCESSING_KEY))) {
            log.warn("Detected leftover data from a previous crash. Processing '{}' first...", TEMP_PROCESSING_KEY);
            syncBatchSafely(TEMP_PROCESSING_KEY);
            return;
        }

        if (Boolean.FALSE.equals(redisTemplate.hasKey(RedisKeys.DISTANCE_BUFFER_KEY))) {
            return;
        }

        log.info("Starting distance sync job...");

        // Atomic Rename
        try {
            redisTemplate.rename(RedisKeys.DISTANCE_BUFFER_KEY, TEMP_PROCESSING_KEY);
        } catch (Exception e) {
            log.debug("Rename failed (concurrency or key missing). Skipping.");
            return;
        }

        syncBatchSafely(TEMP_PROCESSING_KEY);
    }

    /**
     * Helper method: Orchestrates the "Process DB -> Then Delete Redis" logic.
     */
    private void syncBatchSafely(String key) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                processBatchLogic(key);
            });

            redisTemplate.delete(key);
            log.info("Batch processed and deleted from Redis: {}", key);

        } catch (Exception e) {
            log.error("DB Sync failed. Data kept in Redis for retry.", e);
        }
    }

    /**
     * Contains the core business logic for DB updates.
     * Executed within a transaction context provided by the caller.
     */
    private void processBatchLogic(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries.isEmpty()) {
            return;
        }

        int count = 0;
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                Long userId = Long.valueOf((String) entry.getKey());
                Double distance = ((Number) entry.getValue()).doubleValue();

                if (distance > 0) {
                    courierProfileRepository.addDistanceToCourier(userId, distance);
                    count++;
                }
            } catch (Exception e) {
                log.error("Skipping invalid data entry for userId: {}. Error: {}", entry.getKey(), e.getMessage());
            }
        }
        log.info("Synced total distances for {} couriers to DB.", count);
    }
}