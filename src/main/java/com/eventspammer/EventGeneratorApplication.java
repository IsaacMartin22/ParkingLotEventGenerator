package com.eventspammer;

import com.eventspammer.config.AppConfig;
import com.eventspammer.core.EventGenerator;
import com.eventspammer.http.ConfigWebServer;
import com.example.parkinglot.sdk.ParkingLotApiClient;

public class EventGeneratorApplication {

    public static void main(String[] args) {
        try {
            ParkingLotApiClient client = new ParkingLotApiClient();
            AppConfig config = new AppConfig();

            try (ConfigWebServer webServer = new ConfigWebServer(config)) {
                webServer.start();

                EventGenerator generator = new EventGenerator(config, client);

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println();
                    System.out.println("Shutdown requested. Stopping EventSpammer...");
                    generator.stop();
                }));

                 generator.start();

                System.out.println("EventSpammer started. Press Ctrl+C to stop.");
            }
            catch (Exception exception) {
                System.err.println("Failed to start EventSpammer.");
                exception.printStackTrace();
                System.exit(1);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}