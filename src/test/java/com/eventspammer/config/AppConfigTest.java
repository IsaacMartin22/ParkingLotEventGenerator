package com.eventspammer.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppConfigTest {

    @Test
    void constructorCreatesValidDefaultConfiguration() {
        AppConfig config = new AppConfig();

        assertEquals("https://api-service-i1ms.onrender.com/api", config.getBaseUrl());
        assertEquals(5, config.getRequestTimeoutSeconds());
        assertEquals(30_000, config.getFixedGetIntervalMillis());
        assertEquals(2, config.getFixedGetUrls().size());
        assertFalse(config.getRequests().isEmpty());
        assertNotNull(config.getRabbitMq());
        assertEquals("event-spam-events", config.getRabbitMq().getQueueName());
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
        assertEquals(true, body.get("occupied").asBoolean());
        assertEquals(1, body.get("section").get("id").asInt());
        assertNotNull(body.get("car").get("color").asText());
        assertNotNull(body.get("car").get("licensePlate").asText());
        assertNotNull(body.get("car").get("make").asText());
        assertNotNull(body.get("car").get("model").asText());
    }
}
