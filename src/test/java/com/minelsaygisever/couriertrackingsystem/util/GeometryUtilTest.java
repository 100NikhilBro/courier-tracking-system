package com.minelsaygisever.couriertrackingsystem.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeometryUtilTest {

    @Test
    @DisplayName("Should calculate distance correctly between two known points (Ataşehir -> Kadıköy)")
    void calculateDistance_ShouldReturnCorrectDistance() {
        // Given
        double lat1 = 40.9923307;
        double lon1 = 29.1244229;

        double lat2 = 40.991115;
        double lon2 = 29.022218;

        // When
        double distance = GeometryUtil.calculateDistance(lat1, lon1, lat2, lon2);

        // Then
        // Should be approximately 8.5-8.7 km -> 8.6 km with 200m offset
        assertThat(distance).isCloseTo(8600.0, org.assertj.core.data.Offset.offset(200.0));
    }

    @Test
    @DisplayName("Should return 0 when coordinates are the same")
    void calculateDistance_SameCoordinates_ShouldReturnZero() {
        double lat = 41.0082;
        double lon = 28.9784;

        double distance = GeometryUtil.calculateDistance(lat, lon, lat, lon);

        assertThat(distance).isEqualTo(0.0);
    }
}