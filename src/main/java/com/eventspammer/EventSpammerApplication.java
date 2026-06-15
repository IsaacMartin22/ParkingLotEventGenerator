package com.eventspammer;

import com.eventspammer.config.AppConfig;
import com.eventspammer.core.EventSpammer;
import com.eventspammer.http.ApiClient;
import com.eventspammer.rabbitmq.RabbitMqEventPublisher;

public class EventSpammerApplication {

    public static void main(String[] args) {
        try {
            AppConfig config = new AppConfig();
            ApiClient apiClient = new ApiClient(config);

            try (RabbitMqEventPublisher eventPublisher = new RabbitMqEventPublisher(config.getRabbitMq())) {
                eventPublisher.start();

                EventSpammer spammer = new EventSpammer(config, apiClient, eventPublisher);

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println();
                    System.out.println("Shutdown requested. Stopping EventSpammer...");
                    spammer.stop();
                }));

                spammer.start();
            }
        } catch (Exception exception) {
            System.err.println("Failed to start EventSpammer.");
            exception.printStackTrace();
            System.exit(1);
        }
    }
}