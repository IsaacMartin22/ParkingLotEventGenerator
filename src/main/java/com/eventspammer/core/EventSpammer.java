package com.eventspammer.core;

import com.eventspammer.config.AppConfig;
import com.eventspammer.config.RequestDefinition;
import com.eventspammer.http.ApiClient;
import com.eventspammer.http.ApiResponse;
import com.eventspammer.rabbitmq.EventSpamMessage;
import com.eventspammer.rabbitmq.RabbitMqEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class EventSpammer {

    private final AppConfig config;
    private final ApiClient apiClient;
    private final RabbitMqEventPublisher eventPublisher;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public EventSpammer(AppConfig config, ApiClient apiClient, RabbitMqEventPublisher eventPublisher) {
        this.config = config;
        this.apiClient = apiClient;
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

                try {
                    ApiResponse response = apiClient.send(request);
                    sentRequests++;

                    System.out.printf(
                            "[%s] #%d %s %s -> HTTP %d in %dms%n",
                            Instant.now(),
                            sentRequests,
                            request.getMethod(),
                            request.getPath(),
                            response.statusCode(),
                            response.durationMillis()
                    );

                    if (!response.body().isBlank()) {
                        System.out.println("Response body: " + response.body());
                    }

                    eventPublisher.publish(new EventSpamMessage(
                            Instant.now(),
                            sentRequests,
                            request.getName(),
                            request.getMethod().name(),
                            request.getPath(),
                            request.getBody(),
                            true,
                            response.statusCode(),
                            response.durationMillis(),
                            response.body(),
                            null
                    ));
                } catch (Exception exception) {
                    sentRequests++;

                    System.err.printf(
                            "[%s] #%d %s %s -> FAILED: %s%n",
                            Instant.now(),
                            sentRequests,
                            request.getMethod(),
                            request.getPath(),
                            exception.getMessage()
                    );

                    eventPublisher.publish(new EventSpamMessage(
                            Instant.now(),
                            sentRequests,
                            request.getName(),
                            request.getMethod().name(),
                            request.getPath(),
                            request.getBody(),
                            false,
                            null,
                            null,
                            null,
                            exception.getMessage()
                    ));
                }

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

            try {
                ApiResponse response = apiClient.sendGet(fixedGetUrl);

                System.out.printf(
                        "[%s] [fixed-get] #%d GET %s -> HTTP %d in %dms%n",
                        Instant.now(),
                        attemptNumber,
                        fixedGetUrl,
                        response.statusCode(),
                        response.durationMillis()
                );

                eventPublisher.publish(new EventSpamMessage(
                        Instant.now(),
                        attemptNumber,
                        "fixed-get",
                        "GET",
                        fixedGetUrl,
                        null,
                        true,
                        response.statusCode(),
                        response.durationMillis(),
                        response.body(),
                        null
                ));
            } catch (Exception exception) {
                System.err.printf(
                        "[%s] [fixed-get] #%d GET %s -> FAILED: %s%n",
                        Instant.now(),
                        attemptNumber,
                        fixedGetUrl,
                        exception.getMessage()
                );

                eventPublisher.publish(new EventSpamMessage(
                        Instant.now(),
                        attemptNumber,
                        "fixed-get",
                        "GET",
                        fixedGetUrl,
                        null,
                        false,
                        null,
                        null,
                        null,
                        exception.getMessage()
                ));
            }
        }
    }

}