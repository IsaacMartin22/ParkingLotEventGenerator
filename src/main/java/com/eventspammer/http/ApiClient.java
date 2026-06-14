package com.eventspammer.http;

import com.eventspammer.config.AppConfig;
import com.eventspammer.config.RequestDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiClient {

    private final AppConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiClient(AppConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
                .build();
    }

    public ApiResponse send(RequestDefinition requestDefinition) throws Exception {
        URI uri = URI.create(config.getBaseUrl() + requestDefinition.getPath());

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()));

        applyHeaders(requestBuilder, requestDefinition);
        applyMethodAndBody(requestBuilder, requestDefinition);

        HttpRequest request = requestBuilder.build();

        Instant startedAt = Instant.now();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long durationMillis = Duration.between(startedAt, Instant.now()).toMillis();

        return new ApiResponse(
                response.statusCode(),
                response.body() == null ? "" : response.body(),
                durationMillis
        );
    }

    private void applyHeaders(HttpRequest.Builder requestBuilder, RequestDefinition requestDefinition) {
        Map<String, String> mergedHeaders = new LinkedHashMap<>();

        if (config.getDefaultHeaders() != null) {
            mergedHeaders.putAll(config.getDefaultHeaders());
        }

        if (requestDefinition.getHeaders() != null) {
            mergedHeaders.putAll(requestDefinition.getHeaders());
        }

        mergedHeaders.forEach(requestBuilder::header);
    }

    private void applyMethodAndBody(
            HttpRequest.Builder requestBuilder,
            RequestDefinition requestDefinition
    ) throws Exception {
        String body = requestDefinition.getBody() == null
                ? ""
                : objectMapper.writeValueAsString(requestDefinition.getBody());

        switch (requestDefinition.getMethod()) {
            case GET -> requestBuilder.GET();
            case DELETE -> {
                if (body.isBlank()) {
                    requestBuilder.DELETE();
                } else {
                    requestBuilder.method("DELETE", HttpRequest.BodyPublishers.ofString(body));
                }
            }
            case POST -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
            case PUT -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body));
            case PATCH -> requestBuilder.method("PATCH", HttpRequest.BodyPublishers.ofString(body));
        }
    }
}
