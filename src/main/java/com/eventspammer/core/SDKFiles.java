package com.eventspammer.core;

import com.eventspammer.config.AppConfig;
import com.eventspammer.config.RequestDefinition;
import com.example.parkinglot.sdk.ParkingLotApiClient;
import com.example.parkinglot.sdk.model.CarCreateRequest;
import com.example.parkinglot.sdk.model.CarUpdateRequest;
import com.example.parkinglot.sdk.model.ParkingSpaceUpdateRequest;
import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class SDKFiles {

    private final AppConfig config;
    private final ParkingLotApiClient parkingLotApiClient;
    private final HttpClient fallbackHttpClient;

    public SDKFiles(AppConfig config) {
        this.config = config;
        this.parkingLotApiClient = new ParkingLotApiClient(config.getBaseUrl());
        this.fallbackHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
                .build();
    }

    public ApiCallResult execute(RequestDefinition requestDefinition) throws Exception {
        if (requestDefinition == null) {
            throw new IllegalArgumentException("requestDefinition is required.");
        }

        String method = requestDefinition.getMethod().name();
        return switch (requestDefinition.getName()) {
            case "post-car" -> invokeCreateCar(requestDefinition, method);
            case "put-car" -> invokeUpdateCar(requestDefinition, method);
            case "put-space-clear-car" -> invokeUpdateParkingSpace(requestDefinition, method);
            default -> throw new IllegalArgumentException(
                    "No SDK handler for request '" + requestDefinition.getName() + "' (" + method + " " + requestDefinition.getPath() + ").");
        };
    }

    public ApiCallResult sendGet(String absoluteUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(absoluteUrl))
                .timeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
                .GET()
                .build();

        return executeHttp(request, "GET", absoluteUrl);
    }

    private ApiCallResult invokeCreateCar(RequestDefinition requestDefinition, String method) throws Exception {
        CarCreateRequest sdkRequest = new CarCreateRequest();
        Map<String, Object> payload = toMap(requestDefinition.getBody());
        setIfPresent(sdkRequest, "setColor", String.class, payload.get("color"));
        setIfPresent(sdkRequest, "setLicensePlate", String.class, payload.get("licensePlate"));
        setIfPresent(sdkRequest, "setMake", String.class, payload.get("make"));
        setIfPresent(sdkRequest, "setModel", String.class, payload.get("model"));
        setIfPresent(sdkRequest, "setManufacturingYear", Integer.class, payload.get("manufacturingYear"));
        setIfPresent(sdkRequest, "setParkingSpaceId", Integer.class, payload.get("parkingSpaceId"));
        return invokeSdkMethod("createCar", new Class<?>[]{CarCreateRequest.class}, new Object[]{sdkRequest}, method, requestDefinition.getPath());
    }

    private ApiCallResult invokeUpdateCar(RequestDefinition requestDefinition, String method) throws Exception {
        CarUpdateRequest sdkRequest = new CarUpdateRequest();
        Map<String, Object> payload = toMap(requestDefinition.getBody());
        setIfPresent(sdkRequest, "setColor", String.class, payload.get("color"));
        setIfPresent(sdkRequest, "setLicensePlate", String.class, payload.get("licensePlate"));
        int carId = extractTrailingId(requestDefinition.getPath());
        return invokeSdkMethod("updateCar", new Class<?>[]{int.class, CarUpdateRequest.class}, new Object[]{carId, sdkRequest}, method, requestDefinition.getPath());
    }

    private ApiCallResult invokeUpdateParkingSpace(RequestDefinition requestDefinition, String method) throws Exception {
        ParkingSpaceUpdateRequest sdkRequest = new ParkingSpaceUpdateRequest();
        Map<String, Object> payload = toMap(requestDefinition.getBody());
        setIfPresent(sdkRequest, "setNumber", String.class, payload.get("number"));
        setIfPresent(sdkRequest, "setClearCar", Boolean.class, payload.get("clearCar"));
        int spaceId = extractTrailingId(requestDefinition.getPath());
        return invokeSdkMethod("updateParkingSpace", new Class<?>[]{int.class, ParkingSpaceUpdateRequest.class}, new Object[]{spaceId, sdkRequest}, method, requestDefinition.getPath());
    }

    private ApiCallResult invokeSdkMethod(String methodName, Class<?>[] parameterTypes, Object[] args, String httpMethod, String path) throws Exception {
        Method method = parkingLotApiClient.getClass().getMethod(methodName, parameterTypes);
        InstantResponse response = InstantResponse.from(method.invoke(parkingLotApiClient, args));
        return new ApiCallResult(httpMethod, path, response.statusCode(), response.body(), response.durationMillis());
    }

    private ApiCallResult executeHttp(HttpRequest request, String method, String path) throws Exception {
        long startedAt = System.currentTimeMillis();
        HttpResponse<String> response = fallbackHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long durationMillis = System.currentTimeMillis() - startedAt;
        return new ApiCallResult(method, path, response.statusCode(), response.body() == null ? "" : response.body(), durationMillis);
    }

    private Map<String, Object> toMap(JsonNode node) {
        if (node == null || node.isNull()) {
            return Map.of();
        }

        Map<String, Object> values = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> values.put(entry.getKey(), entry.getValue().isNumber() ? entry.getValue().numberValue() : entry.getValue().isBoolean() ? entry.getValue().booleanValue() : entry.getValue().isTextual() ? entry.getValue().asText() : entry.getValue().toString()));
        return values;
    }

    private void setIfPresent(Object target, String methodName, Class<?> parameterType, Object value) {
        if (value == null) {
            return;
        }

        try {
            Method method = target.getClass().getMethod(methodName, parameterType);
            method.invoke(target, coerce(value, parameterType));
        } catch (Exception ignored) {
            // Best effort: SDK method shape may vary slightly between versions.
        }
    }

    private Object coerce(Object value, Class<?> parameterType) {
        if (value == null) {
            return null;
        }
        if (parameterType == Integer.class || parameterType == int.class) {
            return value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString());
        }
        if (parameterType == Boolean.class || parameterType == boolean.class) {
            return value instanceof Boolean bool ? bool : Boolean.parseBoolean(value.toString());
        }
        return value.toString();
    }

    private int extractTrailingId(String path) {
        int slash = path.lastIndexOf('/');
        if (slash < 0 || slash == path.length() - 1) {
            throw new IllegalArgumentException("Expected an id at the end of path: " + path);
        }

        return Integer.parseInt(path.substring(slash + 1));
    }

    public record ApiCallResult(String method, String path, int statusCode, String body, long durationMillis) {
    }

    private record InstantResponse(int statusCode, String body, long durationMillis) {
        static InstantResponse from(Object response) {
            if (response == null) {
                return new InstantResponse(200, "", 0L);
            }

            try {
                Method statusCode = response.getClass().getMethod("statusCode");
                Method body = response.getClass().getMethod("body");
                Method durationMillis = response.getClass().getMethod("durationMillis");
                return new InstantResponse(
                        ((Number) statusCode.invoke(response)).intValue(),
                        body.invoke(response) == null ? "" : body.invoke(response).toString(),
                        ((Number) durationMillis.invoke(response)).longValue()
                );
            } catch (Exception ignored) {
                return new InstantResponse(200, response.toString(), 0L);
            }
        }
    }
}
