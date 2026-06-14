package com.eventspammer.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class RequestDefinition {

    private String name;
    private RequestMethod method;
    private String path;
    private boolean enabled = true;
    private int weight = 1;
    private Map<String, String> headers = new HashMap<>();
    private JsonNode body;

    public void validate() {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Request name is required.");
        }

        if (method == null) {
            throw new IllegalArgumentException("Request method is required for request: " + name);
        }

        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Request path is required for request: " + name);
        }

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Request path must start with '/' for request: " + name);
        }

        if (weight <= 0) {
            throw new IllegalArgumentException("Request weight must be > 0 for request: " + name);
        }
    }
}
