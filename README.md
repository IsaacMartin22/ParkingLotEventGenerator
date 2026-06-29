# EventSpammer

`EventSpammer` is a Java 17 tool that continuously generates HTTP traffic.

It runs **two independent traffic streams**:

1. **Weighted randomized requests** from configured `RequestDefinition` entries.
2. **Two fixed GET requests** that are sent every 30 seconds on a separate scheduler.

The service keeps running until you shut it down.

## Current behavior

- Weighted requests are selected by weight from enabled request definitions.
- Weighted requests respect `delayMillis` between attempts.
- Fixed GET requests are not part of weighted selection and ignore `delayMillis`.
- Fixed GET requests run every `fixedGetIntervalMillis` (default: `30000`).
- Events are published to RabbitMQ when RabbitMQ is enabled.

## Requirements

- Java 17
- Maven 3.9+
- (Optional) RabbitMQ, if event publishing is enabled

## Configure

All runtime settings are currently defined in `src/main/java/com/eventspammer/config/AppConfig.java`.

Update these values before running:

- `baseUrl`: base URL for weighted request paths.
- `requests`: weighted randomized request definitions.
- `delayMillis`: delay between weighted requests.
- `fixedGetUrls`: **exactly two absolute URLs** for scheduled GET requests.
- `fixedGetIntervalMillis`: interval for fixed GET scheduler (default `30000`).
- `rabbitMq`: host, port, credentials, and queue settings.

## Build

```powershell
mvn clean package
```

## Run

```powershell
java -jar target/event-spammer-1.0.0.jar
```

Stop with `Ctrl+C` (or any shutdown signal). The app shuts down gracefully.

## Project notes

- Entry point: `src/main/java/com/eventspammer/EventSpammerApplication.java`
- Main loop: `src/main/java/com/eventspammer/core/EventSpammer.java`
- HTTP client: `src/main/java/com/eventspammer/http/ApiClient.java`
- Agent guidance: `.github/copilot-instructions.md`
