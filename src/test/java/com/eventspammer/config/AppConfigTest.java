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
        assertTrue(requestNames.contains("put-event"));
        assertTrue(requestNames.contains("put-event-null-car"));
    }

    @Test
    void refreshRandomizedBodyUpdatesPutEventBody() {
        AppConfig config = new AppConfig();
        RequestDefinition request = config.getRequests().stream()
                .filter(candidate -> "put-event".equals(candidate.getName()))
                .findFirst()
                .orElseThrow();

        config.refreshRandomizedBody(request);

        JsonNode body = request.getBody();

        assertNotNull(body);
        assertTrue(body.get("occupied").asBoolean());
        assertTrue(body.get("section").get("id").asInt() >= 1);
        assertNotNull(body.get("car").get("color").asText());
        assertNotNull(body.get("car").get("licensePlate").asText());
        assertNotNull(body.get("car").get("make").asText());
        assertNotNull(body.get("car").get("model").asText());
    }

    @Test
    void refreshRandomizedBodyUpdatesPutEventNullCarBody() {
        AppConfig config = new AppConfig();
        RequestDefinition request = config.getRequests().stream()
                .filter(candidate -> "put-event-null-car".equals(candidate.getName()))
                .findFirst()
                .orElseThrow();

        config.refreshRandomizedBody(request);

        JsonNode body = request.getBody();

        assertNotNull(body);
        assertFalse(body.get("occupied").asBoolean());
        assertTrue(body.get("section").get("id").asInt() >= 1);
        assertTrue(body.get("car").isNull());
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
        assertNotEquals(oldBody, body);
    }
}
