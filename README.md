# Sensors Monitoring System

A reactive system for monitoring warehouse environmental conditions (temperature and humidity) using sensor data transmitted via UDP, processed through Apache Kafka.

## Architecture

```
┌──────────┐  UDP   ┌───────────────────┐  Kafka  ┌──────────────────────────┐
│  Sensors │ ─────> │ Warehouse Service │ ──────> │ Central Monitoring Service│
│ (netcat) │        │  (UDP → Kafka)    │         │   (Threshold Alarms)     │
└──────────┘        └───────────────────┘         └──────────────────────────┘
```

- **Warehouse Service** — listens for UDP messages from temperature sensors (port 3344) and humidity sensors (port 3355), parses measurements using Kotlin coroutines + Vert.x, and publishes them to Kafka topic `sensor-measurements`.
- **Central Monitoring Service** — consumes measurements from Kafka via suspend functions, evaluates against configured thresholds, and raises alarms in the logs when thresholds are exceeded.

Each service owns its own domain models — no shared modules, no compile-time coupling. The Kafka JSON contract is the only integration point between services.

## Technology Stack

- **Language:** Kotlin 2.3 (JDK 21)
- **Framework:** Quarkus 3.34 (Reactive)
- **Concurrency:** Kotlin Coroutines + Vert.x Coroutines (`coAwait`, structured concurrency)
- **Message Broker:** Apache Kafka (KRaft mode, no Zookeeper)
- **Communication:** UDP (sensor → warehouse), Kafka (warehouse → monitoring)
- **Build:** Gradle multi-module (2 submodules)
- **Containerization:** Docker, Docker Compose

## Sensor Specifications

| Sensor Type | UDP Port | Message Format             | Threshold |
|-------------|----------|----------------------------|-----------|
| Temperature | 3344     | `sensor_id=t1; value=30`   | 35°C      |
| Humidity    | 3355     | `sensor_id=h1; value=40`   | 50%       |

## Prerequisites

- Docker and Docker Compose

## Running the System

### 1. Start all services

Navigate to the project root directory (where `docker-compose.yml` is located):
```bash
cd /path/to/sensors-monitoring
```

Then run:
```bash
docker-compose up --build
```

> **Note:** The first build takes ~2-3 minutes as Docker downloads base images and Gradle resolves all dependencies. Subsequent builds are significantly faster (~10-20s) thanks to Docker layer caching — dependency layers are cached separately from source code, so only code changes trigger a recompilation.

This starts:
- Kafka (KRaft mode, internal on `kafka:29092`)
- Warehouse Service (UDP ports 3344, 3355)
- Central Monitoring Service

### 2. Simulate sensor data

Send temperature reading (normal):
```bash
echo -n "sensor_id=t1; value=30" | nc -u -w1 localhost 3344
```

Send temperature reading (exceeds threshold — triggers alarm):
```bash
echo -n "sensor_id=t1; value=38" | nc -u -w1 localhost 3344
```

Send humidity reading (normal):
```bash
echo -n "sensor_id=h1; value=40" | nc -u -w1 localhost 3355
```

Send humidity reading (exceeds threshold — triggers alarm):
```bash
echo -n "sensor_id=h1; value=55" | nc -u -w1 localhost 3355
```

Send invalid message (wrong format):
```bash
echo -n "hello world" | nc -u -w1 localhost 3344
```

Send partially valid message (missing value):
```bash
echo -n "sensor_id=t1" | nc -u -w1 localhost 3344
```

> **Note:** Sending an empty payload (`echo -n "" | nc -u ...`) will produce no log output. This is expected — `echo -n ""` sends 0 bytes, and UDP packets with an empty payload are silently dropped by the OS network stack before reaching the application.

### 3. Verify the system

Watch warehouse-service logs:
```bash
docker-compose logs -f warehouse-service
```

Watch central-monitoring-service logs:
```bash
docker-compose logs -f central-monitoring-service
```

Or watch both at once:
```bash
docker-compose logs -f warehouse-service central-monitoring-service
```

**Normal reading** — warehouse-service log:
```
Publishing: sensor=t1, type=TEMPERATURE, value=30.0
```
central-monitoring-service log:
```
Received: sensor=t1, type=TEMPERATURE, value=30.0°C, warehouse=warehouse-1
Within range: sensor=t1, value=30.0°C
```

**Threshold exceeded** — warehouse-service log:
```
Publishing: sensor=t1, type=TEMPERATURE, value=38.0
```
central-monitoring-service log:
```
Received: sensor=t1, type=TEMPERATURE, value=38.0°C, warehouse=warehouse-1
ALARM: TEMPERATURE 38.0°C exceeds threshold 35.0°C [sensor=t1, warehouse=warehouse-1]
```

**Invalid message format** — warehouse-service log (message is rejected, never reaches Kafka):
```
WARN  Parse failed [temperature]: Message does not match expected format | input='hello world'
```

**Partially valid message** — warehouse-service log:
```
WARN  Parse failed [temperature]: Message does not match expected format | input='sensor_id=t1'
```

### 4. Verify Kafka

List topics:
```bash
docker exec kafka kafka-topics --bootstrap-server kafka:29092 --list
```

Read messages from the topic:
```bash
docker exec kafka kafka-console-consumer --bootstrap-server kafka:29092 --topic sensor-measurements --from-beginning
```

### 5. Stop the system

```bash
docker-compose down
```

## Running Tests Locally

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew test
```

## Project Structure

```
sensors-monitoring/
├── warehouse-service/              # UDP listener + Kafka producer
│   ├── src/main/kotlin/
│   │   └── com/beamtrail/warehouse/
│   │       ├── model/
│   │       │   ├── SensorMeasurement.kt    # Core measurement data class
│   │       │   ├── SensorType.kt           # Enum with unit of measure
│   │       │   ├── ValueTypes.kt           # @JvmInline value classes (SensorId, WarehouseId)
│   │       │   ├── ParseResult.kt          # Sealed interface + extension functions
│   │       │   └── SensorMessageParser.kt  # UDP message parser
│   │       ├── config/
│   │       │   └── WarehouseConfig.kt      # @ConfigMapping interface
│   │       ├── udp/
│   │       │   └── UdpSensorListener.kt    # Coroutine-based UDP via Vert.x coAwait
│   │       └── kafka/
│   │           └── MeasurementEmitter.kt   # Suspend Kafka emitter
│   ├── src/test/kotlin/
│   │   └── com/beamtrail/warehouse/
│   │       ├── model/
│   │       │   └── SensorMessageParserTest.kt
│   │       ├── config/
│   │       │   └── WarehouseConfigTest.kt
│   │       └── kafka/
│   │           └── MeasurementEmitterTest.kt
│   └── Dockerfile
├── central-monitoring-service/     # Kafka consumer + threshold monitoring
│   ├── src/main/kotlin/
│   │   └── com/beamtrail/monitoring/
│   │       ├── model/
│   │       │   ├── SensorMeasurement.kt    # Deserialization model (own copy)
│   │       │   ├── SensorType.kt           # Enum with unit of measure
│   │       │   └── ValueTypes.kt           # @JvmInline value classes
│   │       ├── config/
│   │       │   └── ThresholdConfig.kt      # @ConfigMapping interface
│   │       ├── alarm/
│   │       │   ├── AlarmResult.kt          # Sealed interface (Triggered/Normal)
│   │       │   └── ThresholdEvaluator.kt   # Pure evaluation logic
│   │       └── consumer/
│   │           └── MeasurementConsumer.kt  # Suspend Kafka consumer
│   ├── src/test/kotlin/
│   │   └── com/beamtrail/monitoring/alarm/
│   │       └── ThresholdEvaluatorTest.kt
│   └── Dockerfile
├── docker-compose.yml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```
