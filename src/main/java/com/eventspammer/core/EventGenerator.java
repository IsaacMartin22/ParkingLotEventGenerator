package com.eventspammer.core;

import com.eventspammer.config.AppConfig;
import com.example.parkinglot.sdk.ParkingLotApiClient;
import com.example.parkinglot.sdk.model.GenerationRequests;
import com.example.parkinglot.sdk.model.requests.ParkingSpaceUpdateRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class EventGenerator {
    private final ParkingLotApiClient client;
    private final AppConfig config;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public EventGenerator(AppConfig config, ParkingLotApiClient client) {
        this.config = config;
        this.client = client;
    }

    public void start() throws InterruptedException {
        running.set(true);

        log.info("EventSpammer started.");
        log.info("Base URL: {}", config.getBaseUrl());

        int sentRequests = 0;

        while (running.get()) {
            sentRequests++;
            GenerationRequests requestType = selectRandomRequest();
            if (requestType != null) {
                try {
                    sendRandomRequest(requestType);
                } catch (Exception e) {
                    log.error("Failed to send request: {}", requestType, e);
                }
            }
            else {
                log.warn("No request type selected");
            }

            if (config.getDelayMillis() > 0) {
                Thread.sleep(config.getDelayMillis());
            }
        }

        log.info("EventSpammer stopped. Total attempts: {}", sentRequests);
    }

    public void stop() {
        running.set(false);
    }

    public GenerationRequests selectRandomRequest() {
        int totalWeight = config.getTotalWeight();
        if (totalWeight == 0) {
            log.warn("Total weight is 0");
            return null;
        }

        int randomWeight = (int) (Math.random() * totalWeight);
        for (Map.Entry<GenerationRequests, Integer> entry : config.getRequestWeightMap().entrySet()) {
            randomWeight -= entry.getValue();
            if (randomWeight < 0) {
                if (Boolean.TRUE.equals(config.getRequestEnabledMap().get(entry.getKey()))) {
                    log.info("Request type {} is enabled, selecting request", entry.getKey());
                    return entry.getKey();
                } else {
                    log.warn("Request type {} is disabled", entry.getKey());
                    return null;
                }
            }
        }

        return null;
    }

    public void sendRandomRequest(GenerationRequests requestType) {
        switch (requestType) {
            case PARKING_SPACE_UPDATE -> sendParkingSpaceUpdateRequest();
            case PARKING_SPACE_DELETE -> sendParkingSpaceDeleteRequest();
            default -> log.warn("Unknown request type: {}", requestType);
        }
    }

    private void sendParkingSpaceUpdateRequest() {
        ParkingSpaceUpdateRequest parkingSpaceUpdateRequest = new ParkingSpaceUpdateRequest();
        parkingSpaceUpdateRequest.setNumber(String.valueOf((int) (Math.random() * config.getUpperSpaceNumberRand())));
        client.updateParkingSpace((int) (Math.random() * config.getUpperSpaceNumberRand()), parkingSpaceUpdateRequest);
        log.info("Sending parking space update request");
    }

    private void sendParkingSpaceDeleteRequest() {
        //client.removeCar((int) (Math.random() * config.getUpperSpaceNumberRand()));
        log.info("Sending parking space delete request");
    }
}