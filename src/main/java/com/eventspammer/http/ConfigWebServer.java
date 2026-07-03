package com.eventspammer.http;

import com.eventspammer.config.AppConfig;
import com.eventspammer.config.AppConfigUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public final class ConfigWebServer implements AutoCloseable {
    private static final String INDEX_RESOURCE = "/static/index.html";
    private static final int DEFAULT_PORT = 8080;

    private final AppConfig config;
    private final ObjectMapper objectMapper;
    private final HttpServer server;
    private final ExecutorService executor;

    public ConfigWebServer(AppConfig config) throws IOException {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.executor = Executors.newCachedThreadPool();
        this.server = HttpServer.create(new InetSocketAddress(resolvePort()), 0);
        this.server.setExecutor(executor);
        this.server.createContext("/api/config", this::handleConfig);
        this.server.createContext("/", this::handleUi);
    }

    public void start() {
        server.start();
        log.info("Config UI available at http://localhost:{}/", server.getAddress().getPort());
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    private void handleUi(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) || (!"/".equals(path) && !"/index.html".equals(path))) {
                sendJson(exchange, 404, Map.of("error", "Not found"));
                exchange.close();
                return;
            }

            byte[] bytes = readResource(INDEX_RESOURCE);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        } finally {
            exchange.close();
        }
    }

    private void handleConfig(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                sendJson(exchange, 200, config.snapshot());
                return;
            }

            if ("PUT".equalsIgnoreCase(method)) {
                AppConfigUpdateRequest update = objectMapper.readValue(exchange.getRequestBody(), AppConfigUpdateRequest.class);
                config.applyUpdate(update);
                sendJson(exchange, 200, config.snapshot());
                return;
            }

            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
        } catch (Exception exception) {
            log.warn("Config request failed", exception);
            sendJson(exchange, 400, Map.of("error", exception.getMessage()));
        } finally {
            exchange.close();
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private byte[] readResource(String resourcePath) throws IOException {
        try (InputStream inputStream = ConfigWebServer.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Missing resource: " + resourcePath);
            }

            return inputStream.readAllBytes();
        }
    }

    private int resolvePort() {
        String portValue = System.getenv("EVENT_SPAMMER_PORT");
        if (portValue == null || portValue.isBlank()) {
            portValue = System.getenv("PORT");
        }

        if (portValue == null || portValue.isBlank()) {
            return DEFAULT_PORT;
        }

        try {
            return Integer.parseInt(portValue.trim());
        } catch (NumberFormatException exception) {
            log.warn("Invalid port '{}' provided; falling back to {}", portValue, DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }
}


