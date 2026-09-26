package com.minelsaygisever.couriertrackingsystem.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RedisKeys {

    // Keeps the courier's last location
    public static String getLastLocationKey(Long userId) {
        return "courier:" + userId + ":last_location";
    }

    // Locks the courier's entry into the store (for the 1-minute rule)
    public static String getStoreEntryKey(Long userId, Long storeId) {
        return "courier:" + userId + ":store:" + storeId;
    }

    // Idempotency key for location events
    public static String getProcessedEventKey(Long userId, long timestamp) {
        return "processed_event:" + userId + ":" + timestamp;
    }

    // Hash Map key that holds the accumulated distances of couriers
    // KEY=courier:distance:buffer, FIELD=userId, VALUE=distanceIncrement
    public static final String DISTANCE_BUFFER_KEY = "courier:distance:buffer";

    public static final String STORE_GEO_KEY = "stores:geo";
}
