package com.eventspammer.config;

import com.example.parkinglot.sdk.model.GenerationRequests;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
public class AppConfig {
    public static volatile List<String> CAR_COLORS = List.of("Red", "Blue", "Black", "White", "Silver", "Green");
    public static volatile Map<String, List<String>> CAR_MAKES_AND_MODELS = Map.ofEntries(
            Map.entry("Toyota", List.of("Corolla", "Camry", "RAV4", "Prius")),
            Map.entry("Honda", List.of("Civic", "Accord", "CR-V", "Pilot")),
            Map.entry("Ford", List.of("F-150", "Escape", "Explorer", "Mustang")),
            Map.entry("Chevrolet", List.of("Silverado", "Malibu", "Equinox", "Tahoe")),
            Map.entry("Nissan", List.of("Sentra", "Altima", "Rogue", "Pathfinder")),
            Map.entry("Hyundai", List.of("Elantra", "Sonata", "Tucson", "Santa Fe")),
            Map.entry("Kia", List.of("Forte", "K5", "Sportage", "Sorento")),
            Map.entry("Mazda", List.of("Mazda3", "Mazda6", "CX-5", "CX-9")),
            Map.entry("Subaru", List.of("Impreza", "Legacy", "Forester", "Outback")),
            Map.entry("Volkswagen", List.of("Jetta", "Passat", "Golf", "Tiguan")),
            Map.entry("BMW", List.of("3 Series", "5 Series", "X3", "X5")),
            Map.entry("Mercedes-Benz", List.of("C-Class", "E-Class", "GLC", "GLE"))
    );
    public static volatile List<String> CAR_MAKES = CAR_MAKES_AND_MODELS.keySet().stream().toList();

    private final String baseUrl;
    private final Map<String, String> defaultHeaders;



    // These ranges keep generated IDs in a realistic seed-data range for existing cars/spaces.
    private volatile int upperSpaceIdRand = 60;
    private volatile int upperCarIdRand = 500;
    private volatile int upperSpaceNumberRand = 99;
    private volatile Map<GenerationRequests, Integer> requestWeightMap;
    private volatile Map<GenerationRequests, Boolean> requestEnabledMap;
    private volatile long delayMillis;
    private volatile int requestTimeoutSeconds;

    public AppConfig() {
        this.baseUrl = "https://api-service-i1ms.onrender.com/api";
        this.delayMillis =  3 * 1000; // 3 Seconds
        this.requestTimeoutSeconds = 5;

        this.defaultHeaders = Map.of(
                "Content-Type", "application/json",
                "Accept", "application/json"
        );

        this.requestWeightMap = Map.of(
                GenerationRequests.PARKING_SPACE_DELETE, 1,
                GenerationRequests.PARKING_SPACE_UPDATE, 1
        );

        this.requestEnabledMap = Map.of(
                GenerationRequests.PARKING_SPACE_DELETE, true,
                GenerationRequests.PARKING_SPACE_UPDATE, true
        );

        validate();
    }

    public synchronized AppConfigSnapshot snapshot() {
        Map<String, String> headers = defaultHeaders == null ? Map.of() : new LinkedHashMap<>(defaultHeaders);
        Map<GenerationRequests, Integer> weights = requestWeightMap == null ? Map.of() : new LinkedHashMap<>(requestWeightMap);
        Map<GenerationRequests, Boolean> enabled = requestEnabledMap == null ? Map.of() : new LinkedHashMap<>(requestEnabledMap);

        return new AppConfigSnapshot(
                baseUrl,
                headers,
                List.copyOf(CAR_COLORS),
                copyCarMakesAndModels(CAR_MAKES_AND_MODELS),
                List.copyOf(CAR_MAKES),
                upperSpaceIdRand,
                upperCarIdRand,
                upperSpaceNumberRand,
                weights,
                enabled,
                delayMillis,
                requestTimeoutSeconds,
                getTotalWeight()
        );
    }

    public synchronized void applyUpdate(AppConfigUpdateRequest update) {
        if (update == null) {
            return;
        }

        List<String> previousCarColors = CAR_COLORS;
        Map<String, List<String>> previousCarMakesAndModels = CAR_MAKES_AND_MODELS;
        List<String> previousCarMakes = CAR_MAKES;
        int previousUpperSpaceIdRand = upperSpaceIdRand;
        int previousUpperCarIdRand = upperCarIdRand;
        int previousUpperSpaceNumberRand = upperSpaceNumberRand;
        Map<GenerationRequests, Integer> previousRequestWeightMap = requestWeightMap;
        Map<GenerationRequests, Boolean> previousRequestEnabledMap = requestEnabledMap;
        long previousDelayMillis = delayMillis;
        int previousRequestTimeoutSeconds = requestTimeoutSeconds;

        try {
            if (update.carColors() != null) {
                CAR_COLORS = List.copyOf(update.carColors());
            }

            if (update.carMakesAndModels() != null) {
                CAR_MAKES_AND_MODELS = copyCarMakesAndModels(update.carMakesAndModels());
                CAR_MAKES = List.copyOf(CAR_MAKES_AND_MODELS.keySet());
            }

            if (update.upperSpaceIdRand() != null) {
                upperSpaceIdRand = update.upperSpaceIdRand();
            }

            if (update.upperCarIdRand() != null) {
                upperCarIdRand = update.upperCarIdRand();
            }

            if (update.upperSpaceNumberRand() != null) {
                upperSpaceNumberRand = update.upperSpaceNumberRand();
            }

            if (update.requestWeightMap() != null) {
                requestWeightMap = copyRequestWeightMap(update.requestWeightMap());
            }

            if (update.requestEnabledMap() != null) {
                requestEnabledMap = copyRequestEnabledMap(update.requestEnabledMap());
            }

            if (update.delayMillis() != null) {
                delayMillis = update.delayMillis();
            }

            if (update.requestTimeoutSeconds() != null) {
                requestTimeoutSeconds = update.requestTimeoutSeconds();
            }

            if (!validate()) {
                throw new IllegalArgumentException("Updated AppConfig is invalid.");
            }
        } catch (RuntimeException exception) {
            CAR_COLORS = previousCarColors;
            CAR_MAKES_AND_MODELS = previousCarMakesAndModels;
            CAR_MAKES = previousCarMakes;
            upperSpaceIdRand = previousUpperSpaceIdRand;
            upperCarIdRand = previousUpperCarIdRand;
            upperSpaceNumberRand = previousUpperSpaceNumberRand;
            requestWeightMap = previousRequestWeightMap;
            requestEnabledMap = previousRequestEnabledMap;
            delayMillis = previousDelayMillis;
            requestTimeoutSeconds = previousRequestTimeoutSeconds;
            throw exception;
        }
    }

    private boolean validate() {
        if (baseUrl == null || baseUrl.isBlank()) {
            log.error("baseUrl is required.");
            return false;
        }

        if (CAR_COLORS == null || CAR_COLORS.isEmpty()) {
            log.error("At least one car color is required.");
            return false;
        }

        if (CAR_MAKES_AND_MODELS == null || CAR_MAKES_AND_MODELS.isEmpty()) {
            log.error("At least one car make and model mapping is required.");
            return false;
        }

        if (CAR_MAKES == null || CAR_MAKES.isEmpty()) {
            log.error("At least one car make is required.");
            return false;
        }

        if (CAR_MAKES_AND_MODELS.values().stream().anyMatch(models -> models == null || models.isEmpty())) {
            log.error("Each car make must have at least one model.");
            return false;
        }

        if (upperSpaceIdRand <= 0 || upperCarIdRand <= 0 || upperSpaceNumberRand <= 0) {
            log.error("upperSpaceIdRand, upperCarIdRand, and upperSpaceNumberRand must be greater than 0.");
            return false;
        }

        if (delayMillis < 0 || delayMillis > 60_000) {
            log.error("delayMillis must be >= 0 and <= 60,000.");
            return false;
        }

        if (requestTimeoutSeconds <= 0 || requestTimeoutSeconds > 60) {
            log.error("requestTimeoutSeconds must be > 0 and <= 60.");
            return false;
        }

        if (requestWeightMap == null || requestWeightMap.isEmpty()) {
            log.error("At least one request-weight mapping is required.");
            return false;
        }

        if (requestEnabledMap == null || requestEnabledMap.isEmpty()) {
            log.error("At least one request-enabled mapping is required.");
            return false;
        }

        if (requestWeightMap.values().stream().anyMatch(weight -> weight == null || weight < 0)) {
            log.error("Request weights must be >= 0.");
            return false;
        }

        int totalWeight = getTotalWeight();
        if (totalWeight <= 0) {
            log.error("Total weight of all requests must be greater than 0");
            return false;
        }

        boolean anyEnabled = getRequestEnabledMap().values().stream().anyMatch(Boolean::booleanValue);
        if (!anyEnabled) {
            log.error("At least one request must be enabled.");
            return false;
        }

        return true;
    }

    public synchronized void validateOrThrow() {
        if (!validate()) {
            throw new IllegalStateException("AppConfig is invalid.");
        }
    }

    public synchronized int getTotalWeight() {
        return getRequestWeightMap().values().stream().mapToInt(Integer::intValue).sum();
    }

    private Map<String, List<String>> copyCarMakesAndModels(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<>();

        source.forEach((make, models) -> copy.put(make, List.copyOf(models)));

        return copy;
    }

    private Map<GenerationRequests, Integer> copyRequestWeightMap(Map<String, Integer> source) {
        Map<GenerationRequests, Integer> copy = new LinkedHashMap<>();

        source.forEach((requestName, weight) -> copy.put(parseRequest(requestName), weight));

        return copy;
    }

    private Map<GenerationRequests, Boolean> copyRequestEnabledMap(Map<String, Boolean> source) {
        Map<GenerationRequests, Boolean> copy = new LinkedHashMap<>();

        source.forEach((requestName, enabled) -> copy.put(parseRequest(requestName), enabled));

        return copy;
    }

    private GenerationRequests parseRequest(String requestName) {
        return GenerationRequests.valueOf(requestName);
    }
}
