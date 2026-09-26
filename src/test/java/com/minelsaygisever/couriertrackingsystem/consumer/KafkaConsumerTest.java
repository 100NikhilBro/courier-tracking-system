package com.minelsaygisever.couriertrackingsystem.consumer;

import com.minelsaygisever.couriertrackingsystem.dto.event.CourierLocationEvent;
import com.minelsaygisever.couriertrackingsystem.service.CourierTrackingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerTest {

    @Mock
    private CourierTrackingService courierTrackingService;

    @InjectMocks
    private KafkaConsumer kafkaConsumer;

    @Test
    void consume_shouldSetTimestampIfNull_andCallProcessLocation() {
        // Given
        CourierLocationEvent event = new CourierLocationEvent();
        event.setUserId(1L);
        event.setLatitude(40.0);
        event.setLongitude(29.0);
        event.setTimestamp(null);

        int partition = 0;

        // When
        kafkaConsumer.consume(event, partition);

        // Then
        assertNotNull(event.getTimestamp(), "Timestamp should be set by consumer when null");

        verify(courierTrackingService).processLocation(
                eq(1L),
                eq(40.0),
                eq(29.0),
                eq(event.getTimestamp())
        );
    }

    @Test
    void consume_shouldIgnoreEvent_whenRequiredFieldsMissing() {
        // Given: userId null
        CourierLocationEvent event = new CourierLocationEvent();
        event.setUserId(null);
        event.setLatitude(40.0);
        event.setLongitude(29.0);
        event.setTimestamp(Instant.now());

        // When
        kafkaConsumer.consume(event, 0);

        // Then: no processLocation call
        verifyNoInteractions(courierTrackingService);
    }
}