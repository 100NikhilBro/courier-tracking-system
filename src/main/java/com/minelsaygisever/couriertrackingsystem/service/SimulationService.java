package com.minelsaygisever.couriertrackingsystem.service;

import com.minelsaygisever.couriertrackingsystem.dto.event.CourierLocationEvent;
import com.minelsaygisever.couriertrackingsystem.dto.simulation.GeoPoint;
import com.minelsaygisever.couriertrackingsystem.dto.simulation.JourneyRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${courier.kafka.topic-name:courier-locations}")
    private String topicName;
    @Async
    public void startJourney(JourneyRequest request) {
        Long userId = request.getUserId();
        int delay = request.getDelayInSeconds() > 0 ? request.getDelayInSeconds() : 2;

        log.info("Starting journey simulation for User {}", userId);

        for (int i = 0; i < request.getPath().size(); i++) {
            GeoPoint point = request.getPath().get(i);
            try {
                CourierLocationEvent event = new CourierLocationEvent(
                        userId,
                        point.getLatitude(),
                        point.getLongitude(),
                        Instant.now()
                );

                sendToKafka(event);
                log.info("Step {}/{}: Moved to [{}, {}]", (i + 1), request.getPath().size(), point.getLatitude(), point.getLongitude());

                TimeUnit.SECONDS.sleep(delay);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Simulation interrupted for User {}", userId);
                break;
            } catch (Exception e) {
                log.error("Error during simulation step", e);
            }
        }
        log.info("Journey simulation finished for User {}", userId);
    }

    public void sendSingleLocation(CourierLocationEvent event) {
        sendToKafka(event);
    }

    private void sendToKafka(CourierLocationEvent event) {
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        kafkaTemplate.send(topicName, String.valueOf(event.getUserId()), event);
    }
}