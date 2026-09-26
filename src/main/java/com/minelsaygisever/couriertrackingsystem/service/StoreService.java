package com.minelsaygisever.couriertrackingsystem.service;

import com.minelsaygisever.couriertrackingsystem.domain.Store;
import com.minelsaygisever.couriertrackingsystem.repository.StoreRepository;
import com.minelsaygisever.couriertrackingsystem.util.RedisKeys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreService {

    private final StoreRepository storeRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Getter
    private Map<Long, Store> storeMap = Collections.emptyMap();

    @Value("${app.store-cache.enabled:true}")
    private boolean storeCacheEnabled;

    @PostConstruct
    public void loadStoresToCache() {
        if (!storeCacheEnabled) {
            return;
        }
        reloadStores();
    }

    public void ensureGeoCacheLoaded() {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(RedisKeys.STORE_GEO_KEY))) {
            log.warn("Redis Geo Key is missing! Reloading stores from DB...");
            reloadStores();
        }
    }

    private synchronized void reloadStores() {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.STORE_GEO_KEY)) && !storeMap.isEmpty()) {
            return;
        }

        try {
            log.info("Loading stores from DB to Redis Geo Index...");
            List<Store> stores = storeRepository.findAll();

            if (stores.isEmpty()) {
                log.warn("No stores found in DB!");
                return;
            }

            this.storeMap = stores.stream()
                    .collect(Collectors.toMap(Store::getId, Function.identity()));

            String geoKey = RedisKeys.STORE_GEO_KEY;
            redisTemplate.delete(geoKey);

            Map<Object, Point> memberCoordinateMap = new HashMap<>();
            for (Store store : stores) {
                memberCoordinateMap.put(
                        store.getId(),
                        new Point(store.getLongitude(), store.getLatitude())
                );
            }
            redisTemplate.opsForGeo().add(geoKey, memberCoordinateMap);

            log.info("Successfully loaded {} stores into Redis Geo Index.", stores.size());
        } catch (Exception e) {
            log.error("Failed to load stores into Redis!", e);
        }
    }
}
