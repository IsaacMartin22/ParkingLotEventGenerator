package com.eventspammer.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class AppConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> CAR_COLORS = List.of("Red", "Blue", "Black", "White", "Silver", "Green");
    private static final Map<String, List<String>> CAR_MAKES_AND_MODELS = Map.ofEntries(
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
    private static final List<String> CAR_MAKES = CAR_MAKES_AND_MODELS.keySet().stream().toList();


    // These constraints are used to target the parking spaces in the first floor of
    // the first lot, I pick those sections to demonstrate the live updates of a SSE on
    // the frontend, which can narrow down to show visuals of specific floors.
    //
    // True random parking lot simulation would obviously affect all floors/sections/spaces
    private static final int UPPER_SPACE_RAND = 60;
    private static final int UPPER_SECTION_RAND = 6;

    private final String apiRenderUrl;
    private final String frontendRenderUrl;
    private final String baseUrl;
    private final long delayMillis;
    private final long fixedGetIntervalMillis;
    private final List<String> fixedGetUrls;
    private final int requestTimeoutSeconds;
    private final Map<String, String> defaultHeaders;
    private final List<RequestDefinition> requests;
    private final RabbitMqConfig rabbitMq;

    public AppConfig() {
        this.frontendRenderUrl = "https://parkinglotfrontend.onrender.com/";
        this.apiRenderUrl = "https://api-service-i1ms.onrender.com";
        this.baseUrl = apiRenderUrl + "/api";
        this.delayMillis =  3 * 1000; // 3 Seconds
        this.fixedGetIntervalMillis = 30 * 1000; // 30 Seconds
        this.fixedGetUrls = List.of(
                frontendRenderUrl,
                apiRenderUrl
        );
        this.requestTimeoutSeconds = 5;

        this.defaultHeaders = Map.of(
                "Content-Type", "application/json",
                "Accept", "application/json"
        );

        this.rabbitMq = createRabbitMqConfig();

        this.requests = List.of(
                createCarRequest(),
                createPutEventRequest(),
                createClearSpaceRequest()
        );

        validate();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public long getDelayMillis() {
        return delayMillis;
    }

    public long getFixedGetIntervalMillis() {
        return fixedGetIntervalMillis;
    }

    public List<String> getFixedGetUrls() {
        return fixedGetUrls;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public Map<String, String> getDefaultHeaders() {
        return defaultHeaders;
    }

    public List<RequestDefinition> getRequests() {
        return requests;
    }

    public RabbitMqConfig getRabbitMq() {
        return rabbitMq;
    }

    public void refreshRandomizedBody(RequestDefinition request) {
        if (request == null || request.getName() == null) {
            return;
        }

        if (request.getName().equals("post-car")) {
            request.setBody(createCarBody());
            return;
        }

        if (request.getName().equals("put-event")) {
            request.setPath("/spaces/" + randomSpaceNumber());
            request.setBody(createPutEventBody());
            return;
        }

        if (request.getName().equals("put-event-null-car")) {
            request.setPath("/spaces/" + randomSpaceNumber());
            request.setBody(createPutEventNullCarBody());
        }
    }

    private RabbitMqConfig createRabbitMqConfig() {
        RabbitMqConfig rabbitMqConfig = new RabbitMqConfig();

        String rabbitMqUri = readTrimmedEnv("RABBITMQ_URI", null);
        boolean enabledByDefault = rabbitMqUri != null;

        rabbitMqConfig.setEnabled(readBooleanEnv("RABBITMQ_ENABLED", enabledByDefault));
        rabbitMqConfig.setUri(rabbitMqUri);
        rabbitMqConfig.setHost(readTrimmedEnv("RABBITMQ_HOST", "localhost"));
        rabbitMqConfig.setPort(readIntEnv("RABBITMQ_PORT", 5672));
        rabbitMqConfig.setUsername(readTrimmedEnv("RABBITMQ_USERNAME", "guest"));
        rabbitMqConfig.setPassword(readRawEnv("RABBITMQ_PASSWORD", "guest"));
        rabbitMqConfig.setQueueName(readTrimmedEnv("RABBITMQ_QUEUE", "event-spam-events"));

        return rabbitMqConfig;
    }

    private String readTrimmedEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null) {
            return defaultValue;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private String readRawEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null ? defaultValue : value;
    }

    private int readIntEnv(String key, int defaultValue) {
        String value = readTrimmedEnv(key, null);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private boolean readBooleanEnv(String key, boolean defaultValue) {
        String value = readTrimmedEnv(key, null);
        if (value == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    private RequestDefinition createCarRequest() {
        RequestDefinition request = new RequestDefinition();

        request.setName("post-car");
        request.setMethod(RequestMethod.POST);
        request.setPath("/cars");
        request.setEnabled(true);
        // Prod already has over 400 cars, no need to make more for now
        request.setWeight(0);
        request.setBody(createCarBody());

        return request;
    }

    private RequestDefinition createPutEventRequest() {
        RequestDefinition request = new RequestDefinition();

        request.setName("put-event");
        request.setMethod(RequestMethod.PUT);
        request.setPath("/spaces/" + randomSpaceNumber());
        request.setEnabled(true);
        request.setWeight(5);
        request.setBody(createPutEventBody());

        return request;
    }

    private RequestDefinition createClearSpaceRequest() {
        RequestDefinition request = new RequestDefinition();

        request.setName("put-event-null-car");
        request.setMethod(RequestMethod.PUT);
        request.setPath("/spaces/" + randomSpaceNumber());
        request.setEnabled(true);
        request.setWeight(1);
        request.setBody(createPutEventNullCarBody());

        return request;
    }

    private JsonNode createCarBody() {
        CarSpec carSpec = randomCarSpec();

        return OBJECT_MAPPER.valueToTree(Map.of(
                "color", carSpec.color(),
                "licensePlate", randomLicensePlate(),
                "make", carSpec.make(),
                "model", carSpec.model(),
                "manufacturingYear", ThreadLocalRandom.current().nextInt(1990, 2024)
        ));
    }

    private JsonNode createPutEventBody() {
        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("occupied", true);
        body.set("section", OBJECT_MAPPER.valueToTree(Map.of("id", randomSectionId())));
        body.set("car", createCarBody());
        return body;
    }

    private JsonNode createPutEventNullCarBody() {
        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("occupied", false);
        body.set("section", OBJECT_MAPPER.valueToTree(Map.of("id", randomSectionId())));
        body.putNull("car");
        return body;
    }

    private CarSpec randomCarSpec() {
        String color = CAR_COLORS.get(ThreadLocalRandom.current().nextInt(CAR_COLORS.size()));
        String make = CAR_MAKES.get(ThreadLocalRandom.current().nextInt(CAR_MAKES.size()));
        List<String> models = CAR_MAKES_AND_MODELS.get(make);
        String model = models.get(ThreadLocalRandom.current().nextInt(models.size()));

        return new CarSpec(color, make, model);
    }

    private int randomSpaceNumber() {
        return ThreadLocalRandom.current().nextInt(0, UPPER_SPACE_RAND);
    }

    private int randomSectionId() {
        return ThreadLocalRandom.current().nextInt(1, UPPER_SECTION_RAND + 1);
    }

    private String randomLicensePlate() {
        return "%s-%03d".formatted(randomLetters(3), ThreadLocalRandom.current().nextInt(100, 1000));
    }

    private String randomLetters(int length) {
        StringBuilder builder = new StringBuilder();

        for (int index = 0; index < length; index++) {
            char letter = (char) ThreadLocalRandom.current().nextInt('A', 'Z' + 1);
            builder.append(letter);
        }

        return builder.toString();
    }

    private void validate() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl is required.");
        }

        if (delayMillis < 0) {
            throw new IllegalArgumentException("delayMillis must be >= 0.");
        }

        if (fixedGetIntervalMillis <= 0) {
            throw new IllegalArgumentException("fixedGetIntervalMillis must be > 0.");
        }

        if (fixedGetUrls == null || fixedGetUrls.size() != 2) {
            throw new IllegalArgumentException("Exactly two fixed GET URLs are required.");
        }

        for (String fixedGetUrl : fixedGetUrls) {
            if (fixedGetUrl == null || fixedGetUrl.isBlank()) {
                throw new IllegalArgumentException("Fixed GET URLs must be non-blank.");
            }
        }


        if (requestTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("requestTimeoutSeconds must be > 0.");
        }

        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one request definition is required.");
        }

        if (rabbitMq == null) {
            throw new IllegalArgumentException("rabbitMq config is required.");
        }

        rabbitMq.validate();

        for (RequestDefinition request : requests) {
            request.validate();
        }
    }

    private record CarSpec(String color, String make, String model) {
    }
}
