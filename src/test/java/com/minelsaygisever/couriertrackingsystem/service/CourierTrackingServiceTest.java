package com.minelsaygisever.couriertrackingsystem.service;

import com.minelsaygisever.couriertrackingsystem.domain.CourierProfile;
import com.minelsaygisever.couriertrackingsystem.domain.Store;
import com.minelsaygisever.couriertrackingsystem.domain.StoreEntryLog;
import com.minelsaygisever.couriertrackingsystem.dto.courier.CourierLastLocation;
import com.minelsaygisever.couriertrackingsystem.repository.CourierProfileRepository;
import com.minelsaygisever.couriertrackingsystem.repository.StoreEntryLogRepository;
import com.minelsaygisever.couriertrackingsystem.util.RedisKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourierTrackingServiceTest {

    @Mock
    private StoreService storeService;
    @Mock
    private CourierProfileRepository courierProfileRepository;
    @Mock
    private StoreEntryLogRepository storeEntryLogRepository;

    @Mock
    private CourierManagementService courierManagementService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private GeoOperations<String, Object> geoOperations;

    @InjectMocks
    private CourierTrackingService courierTrackingService;

    @BeforeEach
    void setUp() {
        // Return mock operations when calling Redis template
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        lenient().when(courierManagementService.isActiveCourier(anyLong())).thenReturn(true);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }

    @Test
    @DisplayName("Should calculate distance and increment Redis buffer when previous location exists")
    void processLocation_ShouldCalculateDistance_AndBufferIt() {
        // Given
        Long userId = 1L;

        double prevLat = 40.9923307;
        double prevLon = 29.1244229;

        double newLat = 40.993000;
        double newLon = 29.125000;

        Instant now = Instant.now();
        long pastTimestamp = now.minusSeconds(300).toEpochMilli();

        CourierLastLocation lastLoc = new CourierLastLocation(prevLat, prevLon, pastTimestamp);

        // there is an old location in Redis
        when(valueOperations.get(RedisKeys.getLastLocationKey(userId)))
                .thenReturn(lastLoc);

        when(geoOperations.radius(eq(RedisKeys.STORE_GEO_KEY), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(new GeoResults<>(Collections.emptyList()));

        // When
        courierTrackingService.processLocation(userId, newLat, newLon, now);

        // Then
        // distance added to Redis?
        verify(hashOperations).increment(
                eq(RedisKeys.DISTANCE_BUFFER_KEY),
                eq(String.valueOf(userId)),
                anyDouble() // must be greater than 0
        );

        // latest location updated?
        verify(valueOperations).set(eq(RedisKeys.getLastLocationKey(userId)), any(CourierLastLocation.class));
    }

    @Test
    @DisplayName("Should LOG entry and LOCK redis when courier enters store radius for the first time")
    void processLocation_ShouldLogStoreEntry_WhenNotLocked() {
        // Given
        Long userId = 1L;
        Long storeId = 5L;
        // 0 meters distance
        double lat = 40.9923307;
        double lon = 29.1244229;
        Instant now = Instant.now();

        Store mockStore = Store.builder().id(storeId).name("Grand Central Supermarket").latitude(lat).longitude(lon).build();
        CourierProfile mockProfile = CourierProfile.builder().id(99L).build();

        when(storeService.getStoreMap()).thenReturn(Map.of(storeId, mockStore));

        RedisGeoCommands.GeoLocation<Object> geoLocation = new RedisGeoCommands.GeoLocation<>(storeId, new org.springframework.data.geo.Point(lon, lat));
        GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult = new GeoResult<>(geoLocation, new Distance(10)); // 10 metre mesafe
        GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults = new GeoResults<>(List.of(geoResult));

        when(geoOperations.radius(eq(RedisKeys.STORE_GEO_KEY), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(geoResults);
        when(courierProfileRepository.findByUserId(userId)).thenReturn(Optional.of(mockProfile));

        // When
        courierTrackingService.processLocation(userId, lat, lon, now);

        // Then
        // Lock Redis (1 minute)
        verify(valueOperations).set(
                eq(RedisKeys.getStoreEntryKey(userId, storeId)),
                anyLong(), // Epoch
                any(Duration.class) // TTL
        );

        // DB Log
        verify(storeEntryLogRepository).save(any(StoreEntryLog.class));
    }

    @Test
    @DisplayName("Should IGNORE entry if Redis lock exists (Re-entry logic)")
    void processLocation_ShouldIgnoreEntry_WhenLocked() {
        // Given
        Long userId = 1L;
        Long storeId = 5L;
        double lat = 40.9923307;
        double lon = 29.1244229;
        Instant now = Instant.now();

        Store mockStore = Store.builder().id(storeId).name("Grand Central Supermarket").latitude(lat).longitude(lon).build();

        when(storeService.getStoreMap()).thenReturn(Map.of(storeId, mockStore));

        RedisGeoCommands.GeoLocation<Object> geoLocation = new RedisGeoCommands.GeoLocation<>(storeId, new org.springframework.data.geo.Point(lon, lat));
        GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult = new GeoResult<>(geoLocation, new Distance(10));
        GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults = new GeoResults<>(List.of(geoResult));

        when(geoOperations.radius(eq(RedisKeys.STORE_GEO_KEY), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(geoResults);

        long recentEntry = now.minusSeconds(1).toEpochMilli();
        when(valueOperations.get(RedisKeys.getStoreEntryKey(userId, storeId))).thenReturn(recentEntry);

        // When
        courierTrackingService.processLocation(userId, lat, lon, now);

        // Then
        // No new Redis lock
        verify(valueOperations, never()).set(
                eq(RedisKeys.getStoreEntryKey(userId, storeId)),
                any(),
                any(Duration.class)
        );

        // 2. No DB Log
        verify(storeEntryLogRepository, never()).save(any());
    }
}