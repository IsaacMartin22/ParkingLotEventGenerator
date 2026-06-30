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
- `rabbitMq`: host/port credentials or a broker URL, plus queue settings.

Default weighted requests in `AppConfig` now target:

- `POST /api/cars`
  - body keys: `color`, `make`, `model`, `manufacturingYear`, `licensePlate`, `parkingSpaceId`
- `PUT /api/cars/{id}`
  - body keys: `color`, `licensePlate`
- `PUT /api/spaces/{id}`
  - body keys: `number`, `clearCar`

RabbitMQ can now be configured via environment variables (useful for deployments):

- `RABBITMQ_URI`: full `amqp://` or `amqps://` connection URL from your provider.
- `RABBITMQ_ENABLED`: optional (`true`/`false`). Defaults to `true` when `RABBITMQ_URI` is set, otherwise `false`.
- `RABBITMQ_QUEUE`: queue name (default `event-spam-events`).
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`: used when `RABBITMQ_URI` is not provided.

## Build

```powershell
mvn clean package
```

## Run

```powershell
java -jar target/event-spammer-1.0.0.jar
```

Stop with `Ctrl+C` (or any shutdown signal). The app shuts down gracefully.

## Run with Docker (App + RabbitMQ)

Use Docker Compose to run both services together:

```powershell
docker compose up --build
```

What this does:

- Starts RabbitMQ (`rabbitmq:3.13-management`) with credentials `eventspammer` / `eventspammer`.
- Starts this app and injects Docker-networked RabbitMQ settings:
  - `RABBITMQ_ENABLED=true`
  - `RABBITMQ_URI=amqp://eventspammer:eventspammer@rabbitmq:5672/`
  - `RABBITMQ_QUEUE=event-spam-events`
- Exposes RabbitMQ ports:
  - AMQP: `5672`
  - Management UI: `15672`

Stop and remove containers:

```powershell
docker compose down
```

## Project notes

- Entry point: `src/main/java/com/eventspammer/EventSpammerApplication.java`
- Main loop: `src/main/java/com/eventspammer/core/EventSpammer.java`
- HTTP client: `src/main/java/com/eventspammer/http/ApiClient.java`
- Agent guidance: `.github/copilot-instructions.md`
