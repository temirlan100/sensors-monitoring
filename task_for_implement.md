## Overview:
There is a warehouse equipped with various types of sensors that monitor environmental
conditions. These sensors provide measurements such as current temperature and
humidity, which are transmitted via UDP. The warehouse service interacts with all these
sensors and automatically publishes the measurements to a central monitoring service. This
service oversees multiple warehouses and activates an alarm if temperature or humidity
readings exceed configured thresholds.

## System Design:
Your task is to design a reactive system that includes:
- Warehouse Service: Collects data from various sensors and sends it to the Central
  Monitoring Service.
- Central Monitoring Service: Configured with thresholds for temperature and
  humidity. Raises an alarm when sensor measurements cross these thresholds. The
  alarm message should be visible in the logs/console.


## Specifications:
- Sensor Types: Temperature, Humidity
- Communication: Measurements are sent via UDP.
- Central Service Features: Threshold monitoring, alarm activation.

## Technical Requirements:
- Temperature Sensor:
  o UDP Port: 3344
  o Measurement Syntax: sensor_id=t1; value=30
  o Threshold: 35°C
- Humidity Sensor:
  o UDP Port: 3355
  o Measurement Syntax: sensor_id=h1; value=40
  o Threshold: 50%

## Development Expectations:
- No user interactions are required.
- A simple command line/console output is sufficient; no GUI is needed.
- Consider adding test coverage.
- Sensors can be simulated using any utility capable of sending UDP messages, such as netcat.
- Use Apache Kafka as a message broker.
- Provide a docker-compose.yml that includes:
  - Kafka
  - Warehouse Service
  - Central Monitoring Service
- Each service must be implemented as a separate Gradle submodule within a single multi-module repository, each with its own Dockerfile.
- Consider adding test coverage if possible.
- Create a comprehensive README.md that includes:
  - Project overview and architecture description
  - Technology stack used
  - Step-by-step instructions to run the system using docker-compose
  - Instructions on how to simulate sensor data (via UDP)
  - Example commands (CLI/netcat) to test the system end-to-end
  - How to verify that Kafka and services are working correctly
- Use Kotlin version 2 as the primary programming language and Quarkus and JDK 21.
