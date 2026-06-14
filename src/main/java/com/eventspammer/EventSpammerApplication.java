package com.eventspammer;

import com.eventspammer.config.AppConfig;
import com.eventspammer.core.EventSpammer;
import com.eventspammer.http.ApiClient;
import com.eventspammer.util.JsonFileLoader;

import java.nio.file.Path;

public class EventSpammerApplication {

    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : "src/main/resources/EventSpammerConfig.json";

        try {
            AppConfig config = JsonFileLoader.load(Path.of(configPath), AppConfig.class);
            ApiClient apiClient = new ApiClient(config);
            EventSpammer spammer = new EventSpammer(config, apiClient);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println();
                System.out.println("Shutdown requested. Stopping EventSpammer...");
                spammer.stop();
            }));

            spammer.start();
        } catch (Exception exception) {
            System.err.println("Failed to start EventSpammer.");
            exception.printStackTrace();
            System.exit(1);
        }
    }
}
