package com.minelsaygisever.couriertrackingsystem.service;

import com.minelsaygisever.couriertrackingsystem.domain.Store;
import com.minelsaygisever.couriertrackingsystem.domain.StoreEntryLog;
import com.minelsaygisever.couriertrackingsystem.dto.courier.CourierLastLocation;
import com.minelsaygisever.couriertrackingsystem.repository.CourierProfileRepository;
import com.minelsaygisever.couriertrackingsystem.repository.StoreEntryLogRepository;
import com.minelsaygisever.couriertrackingsystem.util.GeometryUtil;
import com.minelsaygisever.couriertrackingsystem.util.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierTrackingService {

    private final StoreService storeService;
    private final StoreEntryLogRepository storeEntryLogRepository;
    private final CourierProfileRepository courierProfileRepository;
    private final CourierManagementService courierManagementService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final double STORE_ENTRY_RADIUS_METERS = 100.0;
    private static final Duration RE_ENTRY_COOLDOWN = Duration.ofMinutes(1);
    private static final Duration STORE_ENTRY_TTL = Duration.ofMinutes(30);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(30);

    /**
     * Every location data coming from Kafka triggers this method.
     */
    public void processLocation(Long userId, double latitude, double longitude, Instant eventTime) {
        if (!courierManagementService.isActiveCourier(userId)) {
            return;
        }

        long eventEpoch = eventTime.toEpochMilli();
        String eventKey = RedisKeys.getProcessedEventKey(userId, eventEpoch);

        // atomic lock to prevent processing duplicate events.
        Boolean isFirstProcessing = redisTemplate.opsForValue()
                .setIfAbsent(eventKey, "1", IDEMPOTENCY_TTL);

        if (Boolean.FALSE.equals(isFirstProcessing)) {
            log.debug("Duplicate event detected via Atomic Check. userId={}, ts={}", userId, eventEpoch);
            return;
        }

        try {
            String lastLocKey = RedisKeys.getLastLocationKey(userId);
            CourierLastLocation lastLoc = (CourierLastLocation) redisTemplate.opsForValue().get(lastLocKey);

            if (lastLoc != null) {
                long lastTs = lastLoc.getTimestamp();

                // ignore out-of-order (late) events for distance calc
                if (eventEpoch > lastTs) {
                    updateTotalDistance(userId, latitude, longitude, lastLoc);
                } else {
                    log.debug("Out-of-order event ignored. userId={}", userId);
                }
            }

            checkStoreEntries(userId, latitude, longitude, eventEpoch);

            if (lastLoc == null || eventEpoch > lastLoc.getTimestamp()) {
                saveLastLocation(userId, latitude, longitude, eventEpoch);
            }

        } catch (Exception e) {
            // release lock to allow Kafka retry on failure
            redisTemplate.delete(eventKey);
            log.error("Error processing location, removed idempotency key to allow retry. userId={}", userId, e);
            throw e;
        }
    }

    private void updateTotalDistance(Long userId, double currentLat, double currentLon, CourierLastLocation lastLoc) {
        double lastLat = lastLoc.getLatitude();
        double lastLon = lastLoc.getLongitude();

        double distance = GeometryUtil.calculateDistance(lastLat, lastLon, currentLat, currentLon);

        if (distance > 0) {
            // bBuffer distance in Redis to reduce DB load
            redisTemplate.opsForHash().increment(RedisKeys.DISTANCE_BUFFER_KEY, String.valueOf(userId), distance);

            log.debug("Buffered distance for Courier {}: +{}m", userId, distance);
        }
    }

    private void checkStoreEntries(Long userId, double lat, double lon, long eventEpoch) {
        storeService.ensureGeoCacheLoaded();

        Circle radius = new Circle(
                new Point(lon, lat),
                new Distance(STORE_ENTRY_RADIUS_METERS, Metrics.METERS)
        );

        // fast in-memory Geo lookup using Redis
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .sortAscending()
                .limit(1);

        GeoResults<RedisGeoCommands.GeoLocation<Object>> results =
                redisTemplate.opsForGeo().radius(RedisKeys.STORE_GEO_KEY, radius, args);

        if (results == null || results.getContent().isEmpty()) {
            return;
        }

        GeoResult<RedisGeoCommands.GeoLocation<Object>> closestStoreResult = results.getContent().get(0);

        RedisGeoCommands.GeoLocation<Object> location = closestStoreResult.getContent();
        Long storeId = ((Number) location.getName()).longValue();

        Store store = storeService.getStoreMap().get(storeId);

        if (store != null) {
            processStoreEntry(userId, store, lat, lon, eventEpoch);
        }
    }

    private void processStoreEntry(Long userId, Store store, double lat, double lon, long eventEpoch) {
        String tsKey = RedisKeys.getStoreEntryKey(userId, store.getId());

        Object cachedValue = redisTemplate.opsForValue().get(tsKey);
        Long lastEntryEpoch = (cachedValue instanceof Number) ? ((Number) cachedValue).longValue() : null;

        // ignore re-entries within 1 minute
        if (lastEntryEpoch != null) {
            Duration sinceLastEntry = Duration.between(
                    Instant.ofEpochMilli(lastEntryEpoch),
                    Instant.ofEpochMilli(eventEpoch)
            );

            if (sinceLastEntry.isNegative() || sinceLastEntry.compareTo(RE_ENTRY_COOLDOWN) < 0) {
                log.debug("Skipping store entry (cooldown). user={}, store={}", userId, store.getName());
                return;
            }

        }

        log.info("Courier {} entered store '{}'", userId, store.getName());

        redisTemplate.opsForValue().set(tsKey, eventEpoch, STORE_ENTRY_TTL);

        saveEntryLog(userId, store, lat, lon);
    }

    private void saveLastLocation(Long userId, double lat, double lon, long timestamp) {
        String key = RedisKeys.getLastLocationKey(userId);
        CourierLastLocation location = CourierLastLocation.builder()
                .latitude(lat)
                .longitude(lon)
                .timestamp(timestamp)
                .build();
        redisTemplate.opsForValue().set(key, location);
    }

    private void saveEntryLog(Long userId, Store store, double lat, double lon) {
        courierProfileRepository.findByUserId(userId).ifPresent(profile -> {
            StoreEntryLog logEntry = StoreEntryLog.builder()
                    .courier(profile)
                    .store(store)
                    .actualLatitude(lat)
                    .actualLongitude(lon)
                    .build();

            storeEntryLogRepository.save(logEntry);
        });
    }
}
