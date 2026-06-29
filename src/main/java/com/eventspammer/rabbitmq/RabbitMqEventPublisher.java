package com.eventspammer.rabbitmq;

import com.eventspammer.config.RabbitMqConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

public class RabbitMqEventPublisher implements AutoCloseable {

    private final RabbitMqConfig config;
    private final ObjectMapper objectMapper;
    private Connection connection;
    private Channel channel;

    public RabbitMqEventPublisher(RabbitMqConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    public void start() throws Exception {
        if (!config.isEnabled()) {
            return;
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.getHost());
        factory.setPort(config.getPort());
        factory.setUsername(config.getUsername());
        factory.setPassword(config.getPassword());

        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(config.getQueueName(), true, false, false, null);

        System.out.println("RabbitMQ publishing enabled.");
        System.out.println("RabbitMQ queue: " + config.getQueueName());
    }

    public synchronized void publish(EventSpamMessage message) {
        if (!config.isEnabled()) {
            return;
        }

        try {
            byte[] body = objectMapper.writeValueAsString(message)
                    .getBytes(StandardCharsets.UTF_8);

            channel.basicPublish(
                    "",
                    config.getQueueName(),
                    null,
                    body
            );
        } catch (Exception exception) {
            System.err.println("Failed to publish event spam message to RabbitMQ: " + exception.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }

        if (connection != null && connection.isOpen()) {
            connection.close();
        }
    }
}
