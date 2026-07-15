package com.eventspammer;

import com.example.parkinglot.sdk.ParkingLotApiClient;
import parkinglot.common.request.ParkingSpaceUpdateRequest;

public class EventGeneratorApplication {


    public static void main(String[] args) {
        try {
            ParkingLotApiClient client = new ParkingLotApiClient();

            System.out.println("EventGenerator starting...");
            for (int i = 360; i < 360 + 60; i++) {
                System.out.println("EventGenerator sending request for space number " + (i+1));
                ParkingSpaceUpdateRequest parkingSpaceUpdateRequest = new ParkingSpaceUpdateRequest();
                parkingSpaceUpdateRequest.setColor("Green");
                parkingSpaceUpdateRequest.setMake("Toyota");
                parkingSpaceUpdateRequest.setModel("Camry");
                parkingSpaceUpdateRequest.setManufacturingYear(2000);
                parkingSpaceUpdateRequest.setLicensePlate("ABC-123");

                try {
                    client.updateParkingSpace(i, parkingSpaceUpdateRequest);
                } catch (Exception e) {
                    System.err.println("Failed to update parking space " + (i+1) + ": " + e.getMessage());
                }
            }

            System.out.println("EventGenerator finished");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}