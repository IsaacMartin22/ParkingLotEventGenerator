package com.eventspammer.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.Map;

@Data
public class RequestDefinition {
    private String name;
    private RequestMethod method;
    private String path;
    private int weight;
    private JsonNode body;
    private Map<String, String> headers;

    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Request name is required.");
        }

        if (method == null) {
            throw new IllegalArgumentException("Request method is required for request: " + name);
        }

        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Request path is required for request: " + name);
        }

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Request path must start with '/' for request: " + name);
        }

        if (weight < 0) {
            throw new IllegalArgumentException("Request weight must be >= 0 for request: " + name);
        }
    }
}

