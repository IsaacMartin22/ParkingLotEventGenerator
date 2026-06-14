package com.eventspammer.config;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AppConfig {

    private String baseUrl;
    private boolean continuous;
    private int totalRequests;
    private long delayMillis;
    private int requestTimeoutSeconds;
    private Map<String, String> defaultHeaders = new HashMap<>();
    private List<RequestDefinition> requests = new ArrayList<>();

    public void validate() {
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

        for (RequestDefinition request : requests) {
            request.validate();
        }
    }
}
