package com.eventspammer.core;

import com.eventspammer.config.AppConfig;
import com.eventspammer.config.RequestDefinition;
import com.eventspammer.rabbitmq.EventSpamMessage;
import com.eventspammer.rabbitmq.RabbitMqEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class EventSpammer {

    private final AppConfig config;
    private final SDKFiles sdkFiles;
    private final RabbitMqEventPublisher eventPublisher;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public EventSpammer(AppConfig config, SDKFiles sdkFiles, RabbitMqEventPublisher eventPublisher) {
        this.config = config;
        this.sdkFiles = sdkFiles;
        this.eventPublisher = eventPublisher;
    }

    public void start() throws InterruptedException {
        List<RequestDefinition> enabledRequests = config.getRequests()
                .stream()
                .filter(RequestDefinition::isEnabled)
                .toList();

        if (enabledRequests.isEmpty()) {
            throw new IllegalArgumentException("At least one request must be enabled.");
        }

        RequestSelector requestSelector = new RequestSelector(enabledRequests);

        running.set(true);

        System.out.println("EventSpammer started.");
        System.out.println("Base URL: " + config.getBaseUrl());
        System.out.println("Delay: " + config.getDelayMillis() + "ms");
        System.out.println("Fixed GET URLs (every " + config.getFixedGetIntervalMillis() + "ms): " + config.getFixedGetUrls());
        System.out.println();

        int sentRequests = 0;
        AtomicInteger fixedGetAttempts = new AtomicInteger(0);
        ScheduledExecutorService fixedGetScheduler = Executors.newSingleThreadScheduledExecutor();

        fixedGetScheduler.scheduleAtFixedRate(
                () -> sendFixedGetRequests(fixedGetAttempts),
                0,
                config.getFixedGetIntervalMillis(),
                TimeUnit.MILLISECONDS
        );

        try {
            while (running.get()) {
                RequestDefinition request = requestSelector.next();
                config.refreshRandomizedBody(request);

                sentRequests++;
                processRequest(sentRequests, request);

                if (config.getDelayMillis() > 0) {
                    Thread.sleep(config.getDelayMillis());
                }
            }
        } finally {
            fixedGetScheduler.shutdownNow();
        }

        System.out.println();
        System.out.println("EventSpammer stopped. Total attempts: " + sentRequests);
    }

    public void stop() {
        running.set(false);
    }

    private void sendFixedGetRequests(AtomicInteger fixedGetAttempts) {
        if (!running.get()) {
            return;
        }

        for (String fixedGetUrl : config.getFixedGetUrls()) {
            int attemptNumber = fixedGetAttempts.incrementAndGet();

            processFixedGetRequest(attemptNumber, fixedGetUrl);
        }
    }

    private void processRequest(int attemptNumber, RequestDefinition request) {
        try {
            SDKFiles.ApiCallResult response = sdkFiles.execute(request);
            handleSuccess(
                    attemptNumber,
                    request.getName(),
                    request.getMethod().name(),
                    request.getPath(),
                    request.getBody(),
                    response,
                    false
            );
        } catch (Exception exception) {
            handleFailure(
                    attemptNumber,
                    request.getName(),
                    request.getMethod().name(),
                    request.getPath(),
                    request.getBody(),
                    exception,
                    false
            );
        }
    }

    private void processFixedGetRequest(int attemptNumber, String fixedGetUrl) {
        try {
            SDKFiles.ApiCallResult response = sdkFiles.sendGet(fixedGetUrl);
            handleSuccess(attemptNumber, "fixed-get", "GET", fixedGetUrl, null, response, true);
        } catch (Exception exception) {
            handleFailure(attemptNumber, "fixed-get", "GET", fixedGetUrl, null, exception, true);
        }
    }

    private void handleSuccess(
            int attemptNumber,
            String requestName,
            String method,
            String path,
            JsonNode requestBody,
            SDKFiles.ApiCallResult response,
            boolean fixedGet
    ) {
        Instant now = Instant.now();
        logSuccess(now, attemptNumber, method, path, response, fixedGet);
        publishEvent(now, attemptNumber, requestName, method, path, requestBody, true, response, null);
    }

    private void handleFailure(
            int attemptNumber,
            String requestName,
            String method,
            String path,
            JsonNode requestBody,
            Exception exception,
            boolean fixedGet
    ) {
        Instant now = Instant.now();
        logFailure(now, attemptNumber, method, path, exception, fixedGet);
        publishEvent(now, attemptNumber, requestName, method, path, requestBody, false, null, exception);
    }

    private void logSuccess(
            Instant timestamp,
            int attemptNumber,
            String method,
            String path,
            SDKFiles.ApiCallResult response,
            boolean fixedGet
    ) {
        System.out.printf(
                "[%s] %s#%d %s %s -> HTTP %d in %dms%n",
                timestamp,
                fixedGet ? "[fixed-get] " : "",
                attemptNumber,
                method,
                path,
                response.statusCode(),
                response.durationMillis()
        );

        if (!response.body().isBlank()) {
            System.out.println("Response body: " + response.body());
        }
    }

    private void logFailure(
            Instant timestamp,
            int attemptNumber,
            String method,
            String path,
            Exception exception,
            boolean fixedGet
    ) {
        System.err.printf(
                "[%s] %s#%d %s %s -> FAILED: %s%n",
                timestamp,
                fixedGet ? "[fixed-get] " : "",
                attemptNumber,
                method,
                path,
                exception.getMessage()
        );
    }

    private void publishEvent(
            Instant timestamp,
            int attemptNumber,
            String requestName,
            String method,
            String path,
            JsonNode requestBody,
            boolean success,
            SDKFiles.ApiCallResult response,
            Exception exception
    ) {
        eventPublisher.publish(new EventSpamMessage(
                timestamp,
                attemptNumber,
                requestName,
                method,
                path,
                requestBody,
                success,
                response == null ? null : response.statusCode(),
                response == null ? null : response.durationMillis(),
                response == null ? null : response.body(),
                exception == null ? null : exception.getMessage()
        ));
    }

}