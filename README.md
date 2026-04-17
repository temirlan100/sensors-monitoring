# Sensors Monitoring System

A reactive system for monitoring warehouse environmental conditions using sensor data transmitted via UDP, processed through Apache Kafka.

## Architecture

```
┌──────────┐  UDP   ┌───────────────────┐  Kafka  ┌──────────────────────────┐
│  Sensors │ ─────> │ Warehouse Service │ ──────> │ Central Monitoring Service│
│ (netcat) │        │  (UDP → Kafka)    │         │   (Threshold Alarms)     │
└────���─────┘        └───────���───────────┘         └──────────���───────────────┘
```

- **Warehouse Service** — listens for UDP messages from sensors, parses measurements using Kotlin coroutines + Vert.x, and publishes them to Kafka topic `sensor-measurements`. Sensor types are defined entirely in configuration — no code changes needed.
- **Central Monitoring Service** — consumes measurements from Kafka via suspend functions, evaluates scalar values against configured thresholds, and raises alarms in the logs when thresholds are exceeded. Non-scalar values (location, composite) are logged and passed through without threshold evaluation.

Each service owns its own domain models — no shared modules, no compile-time coupling. The Kafka JSON contract is the only integration point between services.

## Technology Stack

- **Language:** Kotlin 2.3 (JDK 21)
- **Framework:** Quarkus 3.34 (Reactive)
- **Concurrency:** Kotlin Coroutines + Vert.x Coroutines (`coAwait`, structured concurrency)
- **Message Broker:** Apache Kafka (KRaft mode, no Zookeeper)
- **Communication:** UDP (sensor → warehouse), Kafka (warehouse → monitoring)
- **Build:** Gradle multi-module (2 submodules)
- **Containerization:** Docker, Docker Compose

## Sensor Type System

All sensor types are defined in `application.properties` — **zero sensor types are hardcoded**. The system supports four value kinds:

| Value Kind  | Description                   | UDP Value Format                              | Example                                      |
|-------------|-------------------------------|-----------------------------------------------|----------------------------------------------|
| `scalar`    | Single numeric reading        | `<number>`                                    | `30.5`                                       |
| `location`  | Geographic coordinates        | `<lat>,<lon>`                                 | `51.5074,-0.1278`                            |
| `textual`   | Free-form string              | `<any text>`                                  | `FIRE_DETECTED`                              |
| `composite` | Nested JSON structure         | `<json object>`                               | `{"temperature":22.5,"mode":"cooling"}`      |

### Adding a new sensor type (config only)

1. Add to `warehouse-service/application.properties`:

```properties
warehouse.sensors.co2.port=3388
warehouse.sensors.co2.type=CO2
warehouse.sensors.co2.value-kind=scalar
warehouse.sensors.co2.unit=ppm
```

2. Optionally add a threshold in `central-monitoring-service/application.properties`:

```properties
threshold.sensors.CO2=1000.0
```

3. Expose the port in Dockerfile and docker-compose.yml.

No code changes required.

## Configured Sensors

| Sensor      | Type         | UDP Port | Value Kind  | Message Format                                     | Threshold |
|-------------|--------------|----------|-------------|----------------------------------------------------|-----------|
| Temperature | TEMPERATURE  | 3344     | scalar      | `sensor_id=t1; value=30`                           | 35°C      |
| Humidity    | HUMIDITY     | 3355     | scalar      | `sensor_id=h1; value=40`                           | 50%       |
| GPS         | LOCATION     | 3366     | location    | `sensor_id=gps1; value=51.5074,-0.1278`            | —         |
| HVAC Status | HVAC_STATUS  | 3377     | composite   | `sensor_id=hvac1; value={"temperature":22.5,...}`   | —         |

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
- Warehouse Service (UDP ports 3344, 3355, 3366, 3377)
- Central Monitoring Service

### 2. Simulate sensor data

#### Scalar sensors (temperature, humidity)

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

#### Location sensor (GPS coordinates)

Send GPS coordinates:
```bash
echo -n "sensor_id=gps1; value=51.5074,-0.1278" | nc -u -w1 localhost 3366
```

Send another location (warehouse vehicle tracker):
```bash
echo -n "sensor_id=gps2; value=48.8566,2.3522" | nc -u -w1 localhost 3366
```

#### Composite sensor (HVAC system status — nested JSON)

Send simple HVAC status:
```bash
echo -n 'sensor_id=hvac1; value={"temperature":22.5,"mode":"cooling","fanSpeed":3}' | nc -u -w1 localhost 3377
```

Send HVAC status with nested structure (filter subsystem):
```bash
echo -n 'sensor_id=hvac1; value={"temperature":22.5,"mode":"heating","fanSpeed":2,"filters":{"status":"dirty","hoursRemaining":48},"zones":{"A":21.0,"B":23.5,"C":20.0}}' | nc -u -w1 localhost 3377
```

#### Error cases

Send invalid message (wrong format):
```bash
echo -n "hello world" | nc -u -w1 localhost 3344
```

Send partially valid message (missing value):
```bash
echo -n "sensor_id=t1" | nc -u -w1 localhost 3344
```

Send invalid JSON to composite sensor:
```bash
echo -n "sensor_id=hvac1; value=not-json" | nc -u -w1 localhost 3377
```

Send invalid coordinates to location sensor:
```bash
echo -n "sensor_id=gps1; value=not-a-location" | nc -u -w1 localhost 3366
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

#### Scalar readings

**Normal temperature** — warehouse-service:
```
Publishing: sensor=t1, type=TEMPERATURE, value=Scalar(amount=30.0, unit=°C)
```
central-monitoring-service:
```
Received: sensor=t1, type=TEMPERATURE, value=30.0°C, warehouse=warehouse-1
Within range: sensor=t1, value=30.0°C
```

**Threshold exceeded** — warehouse-service:
```
Publishing: sensor=t1, type=TEMPERATURE, value=Scalar(amount=38.0, unit=°C)
```
central-monitoring-service:
```
Received: sensor=t1, type=TEMPERATURE, value=38.0°C, warehouse=warehouse-1
ALARM: TEMPERATURE 38.0°C exceeds threshold 35.0°C [sensor=t1, warehouse=warehouse-1]
```

#### Location reading

warehouse-service:
```
Publishing: sensor=gps1, type=LOCATION, value=Location(latitude=51.5074, longitude=-0.1278)
```
central-monitoring-service:
```
Received: sensor=gps1, type=LOCATION, value=(51.5074, -0.1278), warehouse=warehouse-1
```
> No threshold evaluation — location values are non-scalar, the evaluator skips them.

#### Composite reading (HVAC with nested structure)

warehouse-service:
```
Publishing: sensor=hvac1, type=HVAC_STATUS, value=Composite(fields={temperature=Scalar(amount=22.5, ...), mode=Textual(content=heating), ...})
```
central-monitoring-service:
```
Received: sensor=hvac1, type=HVAC_STATUS, value=temperature=22.5, mode=heating, fanSpeed=2.0, filters=status=dirty, hoursRemaining=48.0, zones=A=21.0, B=23.5, C=20.0, warehouse=warehouse-1
```
> No threshold evaluation — composite values are non-scalar, the evaluator skips them.

#### Error cases

**Invalid message format** — warehouse-service (message is rejected, never reaches Kafka):
```
WARN  Parse failed [temperature]: Message does not match expected format | input='hello world'
```

**Invalid JSON to composite sensor** — warehouse-service:
```
WARN  Parse failed [hvac]: Cannot parse value 'not-json' for sensor type 'HVAC_STATUS' | input='sensor_id=hvac1; value=not-json'
```

**Invalid coordinates to location sensor** — warehouse-service:
```
WARN  Parse failed [gps]: Cannot parse value 'not-a-location' for sensor type 'LOCATION' | input='sensor_id=gps1; value=not-a-location'
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

Example JSON messages on the topic:

Scalar (temperature):
```json
{"sensorId":"t1","type":"TEMPERATURE","value":{"kind":"scalar","amount":30.0,"unit":"°C"},"warehouseId":"warehouse-1"}
```

Location (GPS):
```json
{"sensorId":"gps1","type":"LOCATION","value":{"kind":"location","latitude":51.5074,"longitude":-0.1278},"warehouseId":"warehouse-1"}
```

Composite (HVAC with nested structure):
```json
{"sensorId":"hvac1","type":"HVAC_STATUS","value":{"kind":"composite","fields":{"temperature":{"kind":"scalar","amount":22.5},"mode":{"kind":"textual","content":"cooling"},"fanSpeed":{"kind":"scalar","amount":3.0}}},"warehouseId":"warehouse-1"}
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
│   │       │   ├── SensorType.kt           # Open value class (just a name wrapper)
│   │       │   ├── SensorValue.kt          # Sealed value hierarchy (Scalar, Location, etc.)
│   │       │   ├── SensorTypeDescriptor.kt # Config-driven value parsers per value kind
│   │       │   ├── ValueTypes.kt           # @JvmInline value classes (SensorId, WarehouseId)
│   │       │   ├── ParseResult.kt          # Sealed interface + extension functions
│   │       │   └── SensorMessageParser.kt  # UDP message parser (descriptor-driven)
│   │       ├── config/
│   │       │   └── WarehouseConfig.kt      # @ConfigMapping: port, type, value-kind, unit
│   │       ├── udp/
│   │       │   └── UdpSensorListener.kt    # Builds descriptors from config at startup
│   │       └── kafka/
│   │           └── MeasurementEmitter.kt   # Suspend Kafka emitter
│   ├── src/test/kotlin/
│   └── Dockerfile
├���─ central-monitoring-service/     # Kafka consumer + threshold monitoring
│   ├── src/main/kotlin/
│   │   └── com/beamtrail/monitoring/
│   │       ├── model/
│   │       │   ├── SensorMeasurement.kt    # Deserialization model (own copy)
│   │       │   ├── SensorType.kt           # Open value class (own copy)
│   │       │   ├── SensorValue.kt          # Sealed value hierarchy (own copy)
│   │       │   └── ValueTypes.kt           # @JvmInline value classes
│   │       ├── config/
│   │       │   └── ThresholdConfig.kt      # Dynamic map-based thresholds
│   │       ├── alarm/
│   │       │   ├── AlarmResult.kt          # Sealed interface (Triggered/Normal/Skipped)
│   │       │   └── ThresholdEvaluator.kt   # Scalar threshold evaluation
│   │       └── consumer/
│   │           └── MeasurementConsumer.kt  # Suspend Kafka consumer
│   ├── src/test/kotlin/
│   └── Dockerfile
├── docker-compose.yml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```
