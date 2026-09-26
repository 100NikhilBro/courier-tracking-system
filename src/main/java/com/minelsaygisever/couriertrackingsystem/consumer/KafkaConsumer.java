package com.minelsaygisever.couriertrackingsystem.consumer;

import com.minelsaygisever.couriertrackingsystem.dto.event.CourierLocationEvent;
import com.minelsaygisever.couriertrackingsystem.service.CourierTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class KafkaConsumer {

    private final CourierTrackingService courierTrackingService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "${courier.kafka.topic-name}", groupId = "${courier.kafka.consumer-group-id}")
    public void consume(CourierLocationEvent event, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        log.info("Received Location Event: {} [Partition: {}]", event, partition);

        // Validate required fields
        if (event.getUserId() == null ||
                event.getLatitude() == null ||
                event.getLongitude() == null) {

            log.warn("Ignoring invalid location event: {}", event);
            return;
        }

        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }

        courierTrackingService.processLocation(
                event.getUserId(),
                event.getLatitude(),
                event.getLongitude(),
                event.getTimestamp()
        );
    }

    @DltHandler
    public void handleDlt(CourierLocationEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Event failed after retries and moved to DLT. Topic: {}, Payload: {}", topic, event);
    }
}