package com.eventspammer.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class AppConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final boolean continuous;
    private final int totalRequests;
    private final long delayMillis;
    private final int requestTimeoutSeconds;
    private final Map<String, String> defaultHeaders;
    private final List<RequestDefinition> requests;
    private final RabbitMqConfig rabbitMq;

    public AppConfig() {
        this.baseUrl = "http://localhost:8080/api";
        this.continuous = true;
        this.totalRequests = 1_000_000;
        this.delayMillis = 500;
        this.requestTimeoutSeconds = 5;

        this.defaultHeaders = Map.of(
                "Content-Type", "application/json",
                "Accept", "application/json"
        );

        this.rabbitMq = createRabbitMqConfig();

        this.requests = List.of(
                createPostEventRequest()
        );

        validate();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public long getDelayMillis() {
        return delayMillis;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public Map<String, String> getDefaultHeaders() {
        return defaultHeaders;
    }

    public List<RequestDefinition> getRequests() {
        return requests;
    }

    public RabbitMqConfig getRabbitMq() {
        return rabbitMq;
    }

    private RabbitMqConfig createRabbitMqConfig() {
        RabbitMqConfig rabbitMqConfig = new RabbitMqConfig();

        rabbitMqConfig.setEnabled(true);
        rabbitMqConfig.setHost("localhost");
        rabbitMqConfig.setPort(5672);
        rabbitMqConfig.setUsername("guest");
        rabbitMqConfig.setPassword("guest");
        rabbitMqConfig.setQueueName("event-spam-events");

        return rabbitMqConfig;
    }

    private RequestDefinition createPostEventRequest() {
        RequestDefinition request = new RequestDefinition();

        request.setName("post-event");
        request.setMethod(RequestMethod.POST);
        request.setPath("/spaces");
        request.setEnabled(true);
        request.setWeight(3);
        request.setBody(createPostEventBody());

        return request;
    }

    private JsonNode createPostEventBody() {
        return OBJECT_MAPPER.valueToTree(Map.of(
                "number", randomSpaceNumber(),
                "occupied", true,
                "section", Map.of(
                        "id", randomSectionId()
                ),
                "car", Map.of(
                        "licensePlate", randomLicensePlate(),
                        "brand", "Toyota",
                        "model", "Corolla"
                )
        ));
    }

    private String randomSpaceNumber() {
        char letter = (char) ThreadLocalRandom.current().nextInt('A', 'Z' + 1);
        int number = ThreadLocalRandom.current().nextInt(1, 1000);

        return "%c-%03d".formatted(letter, number);
    }

    private int randomSectionId() {
        return ThreadLocalRandom.current().nextInt(1, 11);
    }

    private String randomLicensePlate() {
        return "%s-%03d".formatted(randomLetters(3), ThreadLocalRandom.current().nextInt(100, 1000));
    }

    private String randomLetters(int length) {
        StringBuilder builder = new StringBuilder();

        for (int index = 0; index < length; index++) {
            char letter = (char) ThreadLocalRandom.current().nextInt('A', 'Z' + 1);
            builder.append(letter);
        }

        return builder.toString();
    }

    private void validate() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl is required.");
        }

        if (delayMillis < 0) {
            throw new IllegalArgumentException("delayMillis must be >= 0.");
        }

        if (!continuous && totalRequests <= 0) {
            throw new IllegalArgumentException("totalRequests must be > 0 when continuous is false.");
        }

        if (requestTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("requestTimeoutSeconds must be > 0.");
        }

        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one request definition is required.");
        }

        if (rabbitMq == null) {
            throw new IllegalArgumentException("rabbitMq config is required.");
        }

        rabbitMq.validate();

        for (RequestDefinition request : requests) {
            request.validate();
        }
    }
}
