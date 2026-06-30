package com.eventspammer.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void constructorCreatesValidDefaultConfiguration() {
        AppConfig config = new AppConfig();

        assertEquals("https://api-service-i1ms.onrender.com/api", config.getBaseUrl());
        assertEquals(5, config.getRequestTimeoutSeconds());
        assertEquals(30_000, config.getFixedGetIntervalMillis());
        assertEquals(2, config.getFixedGetUrls().size());
        assertFalse(config.getRequests().isEmpty());
        assertEquals(3, config.getRequests().size());
        assertNotNull(config.getRabbitMq());
        assertEquals("event-spam-events", config.getRabbitMq().getQueueName());

        Set<String> requestNames = config.getRequests().stream()
                .map(RequestDefinition::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(requestNames.contains("post-car"));
        assertTrue(requestNames.contains("put-car"));
        assertTrue(requestNames.contains("put-space-clear-car"));
    }

    @Test
    void refreshRandomizedBodyUpdatesPutCarBody() {
        AppConfig config = new AppConfig();
        RequestDefinition request = config.getRequests().stream()
                .filter(candidate -> "put-car".equals(candidate.getName()))
                .findFirst()
                .orElseThrow();

        config.refreshRandomizedBody(request);

        JsonNode body = request.getBody();

        assertNotNull(body);
        assertTrue(request.getPath().startsWith("/cars/"));
        assertNotNull(body.get("color").asText());
        assertNotNull(body.get("licensePlate").asText());
        assertTrue(body.get("licensePlate").asText().matches("^[A-Z]{3}-\\d{4}$"));
    }

    @Test
    void refreshRandomizedBodyUpdatesPutSpaceClearCarBody() {
        AppConfig config = new AppConfig();
        RequestDefinition request = config.getRequests().stream()
                .filter(candidate -> "put-space-clear-car".equals(candidate.getName()))
                .findFirst()
                .orElseThrow();

        config.refreshRandomizedBody(request);

        JsonNode body = request.getBody();

        assertNotNull(body);
        assertTrue(request.getPath().startsWith("/spaces/"));
        assertNotNull(body.get("number").asText());
        assertTrue(body.get("number").asText().matches("^[A-Z]-\\d{2}$"));
        assertTrue(body.get("clearCar").asBoolean());
    }

    @Test
    void refreshRandomizedBodyUpdatesPostCarBody() {
        AppConfig config = new AppConfig();
        RequestDefinition request = config.getRequests().stream()
                .filter(candidate -> "post-car".equals(candidate.getName()))
                .findFirst()
                .orElseThrow();

        JsonNode oldBody = request.getBody();
        config.refreshRandomizedBody(request);
        JsonNode body = request.getBody();

        assertNotNull(body);
        assertNotNull(body.get("color").asText());
        assertNotNull(body.get("licensePlate").asText());
        assertNotNull(body.get("make").asText());
        assertNotNull(body.get("model").asText());
        assertTrue(body.get("manufacturingYear").asInt() >= 1990);
        assertTrue(body.get("parkingSpaceId").asInt() >= 1);
        assertNotEquals(oldBody, body);
    }
}
